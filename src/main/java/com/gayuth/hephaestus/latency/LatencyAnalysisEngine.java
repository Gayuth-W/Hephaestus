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


}