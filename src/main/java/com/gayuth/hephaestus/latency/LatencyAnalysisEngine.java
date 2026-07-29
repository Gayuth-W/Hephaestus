package com.gayuth.hephaestus.latency;

import java.util.Comparator;
import java.util.List;

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
}