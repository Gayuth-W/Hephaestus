package com.gayuth.hephaestus.latency;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gayuth.hephaestus.dto.LatencyResultDTO;
import com.gayuth.hephaestus.dto.ServiceLatencyDTO;
import com.gayuth.hephaestus.enums.Confidence;
import com.gayuth.hephaestus.model.SpanNode;
import com.gayuth.hephaestus.model.TraceGraph;

/**
 * Critical-path latency analysis.
 *
 * <p>
 * Self (exclusive) time per span is duration minus the UNION of its child
 * windows, so overlapping children are never double-counted. That part was
 * already right. What was missing is the critical path, and dividing self time
 * by the root's wall clock produced shares that summed to well over 100% (170%
 * on a real checkout trace, 280% on a three-way parallel fan-out) because
 * concurrent siblings each claimed the same milliseconds.
 *
 * <p>
 * Two honest numbers replace that one dishonest one:
 * <ul>
 *   <li>{@code workShare} - self time over TOTAL self time. Sums to 100%.
 *       Answers "where did the work happen".</li>
 *   <li>{@code criticalShare} - self time on the critical path over the length
 *       of the critical path. Sums to 100%. Answers "what made it slow".</li>
 * </ul>
 * A busy service fully overlapped by other work scores high on the first and
 * zero on the second - which is exactly the distinction the tool exists to
 * make. The sink is ranked by critical path time.
 *
 * <p>
 * The critical path is walked backwards from the end of each span: take the
 * child that finished last, then from that child's start take the last child
 * to finish before it, and so on. This follows sequential dependencies between
 * siblings, so a slow call that ran concurrently with a slower one correctly
 * falls off the path.
 */
public final class LatencyAnalysisEngine {

  // How dominant the top service must be on the critical path.
  private static final double DOMINANT_SHARE = 0.50;
  private static final double DOMINANT_GAP = 0.20;
  private static final double AMBIGUOUS_SHARE = 0.34;
  private static final double AMBIGUOUS_GAP = 0.10;

  public LatencyResultDTO analyze(TraceGraph graph) {
    Map<String, Long> selfByService = new LinkedHashMap<>();
    accumulate(graph.root(), selfByService);

    List<SpanNode> critical = new ArrayList<>();
    boolean[] tied = { false };
    walkCriticalPath(graph.root(), critical, tied);

    Map<String, Long> criticalByService = new LinkedHashMap<>();
    Set<String> criticalSpanIds = new LinkedHashSet<>();
    long criticalTotal = 0L;
    for (SpanNode n : critical) {
      long self = exclusiveTime(n);
      criticalByService.merge(n.serviceName(), self, Long::sum);
      criticalSpanIds.add(n.spanId());
      criticalTotal += self;
    }

    long selfTotal = 0L;
    for (long v : selfByService.values()) {
      selfTotal += v;
    }
    double selfDen = Math.max(1L, selfTotal);
    double criticalDen = Math.max(1L, criticalTotal);

    List<ServiceLatencyDTO> breakdown = new ArrayList<>();
    for (Map.Entry<String, Long> e : selfByService.entrySet()) {
      long onPath = criticalByService.getOrDefault(e.getKey(), 0L);
      breakdown.add(new ServiceLatencyDTO(e.getKey(), e.getValue(), e.getValue() / selfDen,
              onPath, onPath / criticalDen));
    }
    breakdown.sort(Comparator
            .comparingLong(ServiceLatencyDTO::criticalPathTime)
            .thenComparingLong(ServiceLatencyDTO::selfTime)
            .reversed()
            .thenComparing(ServiceLatencyDTO::service));

    ServiceLatencyDTO top = breakdown.get(0);
    double topShare = top.criticalShare();
    double secondShare = breakdown.size() > 1 ? breakdown.get(1).criticalShare() : 0.0;
    double gap = topShare - secondShare;

    Confidence confidence;
    String reason;
    if (tied[0]) {
      // Two siblings finished at the same instant, so which one "held up" the
      // parent is arbitrary. Saying HIGH here would be a coin flip in a suit.
      confidence = Confidence.LOW;
      reason = "Concurrent branches finish at the same time, so the critical path is ambiguous"
              + " (nominal top: " + top.service() + " at " + pct(topShare) + ").";
    } else if (breakdown.size() == 1 || (topShare >= DOMINANT_SHARE && gap >= DOMINANT_GAP)) {
      confidence = Confidence.HIGH;
      reason = top.service() + " is the latency sink: " + top.criticalPathTime() + "ms of the "
              + criticalTotal + "ms critical path (" + pct(topShare) + ")." + offPathNote(breakdown);
    } else if (topShare < AMBIGUOUS_SHARE || gap < AMBIGUOUS_GAP) {
      confidence = Confidence.LOW;
      reason = "No single dominant latency sink; the critical path is spread across services (top: "
              + top.service() + " at " + pct(topShare) + ").";
    } else {
      confidence = Confidence.MEDIUM;
      reason = top.service() + " is the likely latency sink at " + pct(topShare)
              + " of the critical path, though not strongly dominant." + offPathNote(breakdown);
    }

    return new LatencyResultDTO(top.service(), topShare, confidence, reason, criticalTotal,
            new ArrayList<>(criticalSpanIds), breakdown);
  }

  /**
   * Names the busiest service that contributes nothing to the critical path.
   * This is the finding a naive "slowest service" ranking gets backwards, so it
   * is worth stating explicitly rather than leaving in a table.
   */
  private static String offPathNote(List<ServiceLatencyDTO> breakdown) {
    ServiceLatencyDTO worst = null;
    for (ServiceLatencyDTO s : breakdown) {
      if (s.criticalPathTime() == 0L && (worst == null || s.selfTime() > worst.selfTime())) {
        worst = s;
      }
    }
    if (worst == null || worst.selfTime() == 0L) {
      return "";
    }
    return " " + worst.service() + " spent " + worst.selfTime()
            + "ms but ran concurrently and is off the critical path, so speeding it up would not"
            + " make the request faster.";
  }

  private void accumulate(SpanNode node, Map<String, Long> acc) {
    acc.merge(node.serviceName(), exclusiveTime(node), Long::sum);
    for (SpanNode child : node.children()) {
      accumulate(child, acc);
    }
  }

  /**
   * Walks the chain of spans that determined when the request finished. At each
   * level: take the last child to finish, recurse into it, then move the cursor
   * back to that child's start and repeat - picking up the work that had to
   * complete before it could begin. Children ending after the cursor were
   * running in parallel and are skipped.
   */
  private void walkCriticalPath(SpanNode node, List<SpanNode> out, boolean[] tied) {
    out.add(node);
    long cursor = node.span().endTime();
    List<SpanNode> remaining = new ArrayList<>(node.children());
    while (true) {
      SpanNode best = null;
      int ties = 0;
      for (SpanNode c : remaining) {
        long end = c.span().endTime();
        if (end > cursor) {
          continue;
        }
        if (best == null || end > best.span().endTime()) {
          best = c;
          ties = 1;
        } else if (end == best.span().endTime()) {
          ties++;
        }
      }
      if (best == null) {
        return;
      }
      if (ties > 1) {
        tied[0] = true;
      }
      remaining.remove(best);
      walkCriticalPath(best, out, tied);
      cursor = best.span().startTime();
    }
  }

  /** Self time of a single span: duration minus the union of its clamped child windows. */
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
      if (iv[0] > curEnd) {
        covered += curEnd - curStart;
        curStart = iv[0];
        curEnd = iv[1];
      } else {
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
