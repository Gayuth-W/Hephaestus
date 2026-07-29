package com.gayuth.hephaestus.latency;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gayuth.hephaestus.dto.LatencyResultDTO;
import com.gayuth.hephaestus.dto.ServiceLatencyDTO;
import com.gayuth.hephaestus.enums.Confidence;
import com.gayuth.hephaestus.model.SpanNode;
import com.gayuth.hephaestus.model.TraceGraph;

/**
 * Critical-path / self-time latency analysis.
 *
 * For each span, EXCLUSIVE (self) time = its own duration minus the time it
 * spent waiting on children. Children can run in parallel, so we subtract the
 * UNION of child windows, never their sum - otherwise overlapping work would be
 * double-counted and self time could go negative.
 *
 *   exclusive(span) = duration - length( union of child windows, clamped to span )
 *
 * Exclusive time is summed per service; the service with the most is the latency
 * sink. Confidence is derived from how dominant that sink is, so a trace with no
 * clear bottleneck honestly reports LOW rather than forcing a pick.
 */
public final class LatencyAnalysisEngine {

  // Tunable heuristics for how "dominant" a sink must be.
  private static final double DOMINANT_SHARE = 0.50;
  private static final double DOMINANT_GAP = 0.20;
  private static final double AMBIGUOUS_SHARE = 0.34;
  private static final double AMBIGUOUS_GAP = 0.10;

  public LatencyResultDTO analyze(TraceGraph graph) {
    long total = Math.max(1L, graph.root().span().duration()); // whole-request wall clock

    Map<String, Long> exclusiveByService = new LinkedHashMap<>();
    accumulate(graph.root(), exclusiveByService);

    List<ServiceLatencyDTO> breakdown = new ArrayList<>();
    for (Map.Entry<String, Long> e : exclusiveByService.entrySet()) {
      breakdown.add(new ServiceLatencyDTO(e.getKey(), e.getValue(), e.getValue() / (double) total));
    }
    breakdown.sort(Comparator
                    .comparingLong(ServiceLatencyDTO::exclusiveTime)
                    .reversed()
                    .thenComparing(ServiceLatencyDTO::service));

    ServiceLatencyDTO top = breakdown.get(0);
    double topShare = top.contribution();
    double secondShare = breakdown.size() > 1 ? breakdown.get(1).contribution() : 0.0;
    double gap = topShare - secondShare;

    Confidence confidence;
    String reason;
    if (breakdown.size() == 1 || (topShare >= DOMINANT_SHARE && gap >= DOMINANT_GAP)) {
      confidence = Confidence.HIGH;
      reason = top.service() + " is the latency sink, spending " + pct(topShare) + " of the request in its own code; its dependencies were not the bottleneck.";
    } else if (topShare < AMBIGUOUS_SHARE || gap < AMBIGUOUS_GAP) {
      confidence = Confidence.LOW;
      reason = "No single dominant latency sink; time is spread across services (top: " + top.service() + " at " + pct(topShare) + ").";
    } else {
      confidence = Confidence.MEDIUM;
      reason = top.service() + " is the likely latency sink at " + pct(topShare) + " of request time, though not strongly dominant.";
    }
    return new LatencyResultDTO(top.service(), topShare, confidence, reason, breakdown);
  }

  private void accumulate(SpanNode node, Map<String, Long> acc) {
    acc.merge(node.serviceName(), exclusiveTime(node), Long::sum);
    for (SpanNode child : node.children()) {
      accumulate(child, acc);
    }
  }

  /** Self time of a single span: duration minus the union of its (clamped) child windows. */
  public static long exclusiveTime(SpanNode node) {
    long start = node.span().startTime();
    long end = node.span().endTime();
    long duration = node.span().duration();
    if (node.children().isEmpty()) {
      return Math.max(0L, duration);
    }
    List<long[]> intervals = new ArrayList<>();
    for (SpanNode child : node.children()) {
      long cs = Math.max(start, child.span().startTime());
      long ce = Math.min(end, child.span().endTime());
      if (ce > cs) {
        intervals.add(new long[] { cs, ce });
      }
    }
    return Math.max(0L, duration - unionLength(intervals));
  }

  /** Total length covered by a set of intervals, counting overlaps once. */
  static long unionLength(List<long[]> intervals) {
    if (intervals.isEmpty()) {
      return 0L;
    }
    intervals.sort(Comparator.comparingLong(iv -> iv[0]));
    long covered = 0L;
    long curStart = intervals.get(0)[0];
    long curEnd = intervals.get(0)[1];
    for (int i = 1; i < intervals.size(); i++) {
      long[] iv = intervals.get(i);
      if (iv[0] > curEnd) { // disjoint: close the current run
        covered += curEnd - curStart;
        curStart = iv[0];
        curEnd = iv[1];
      } else { // overlapping or touching: extend
        curEnd = Math.max(curEnd, iv[1]);
      }
    }
    covered += curEnd - curStart;
    return covered;
  }

  private static String pct(double share) {
    return String.format("%.1f%%", share * 100);
  }
}