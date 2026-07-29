package com.gayuth.hephaestus.latency;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.gayuth.hephaestus.model.SpanNode;

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


  private void accumulate(SpanNode node, Map<String, Long> acc) {
    acc.merge(node.serviceName(), exclusiveTime(node), Long::sum);
    for (SpanNode child : node.children()) {
      accumulate(child, acc);
    }
  }

  /** Self time of a single span: duration minus the union of its (clamped) child windows. */
  static long exclusiveTime(SpanNode node) {
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