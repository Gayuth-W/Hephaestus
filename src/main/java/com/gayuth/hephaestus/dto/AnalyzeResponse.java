package com.gayuth.hephaestus.dto;

import java.util.List;

/**
 * Everything the UI needs to render one analysis.
 *
 * <p>
 * Both {@code failure} and {@code latency} are ALWAYS populated. A failing
 * trace is usually also a slow trace, and the reason it was slow is often the
 * most actionable output; running only one engine threw that away.
 * {@code mode} is now a display hint for which verdict the UI leads with.
 */
public record AnalyzeResponse(
    String traceId,
    String mode, // FAILURE or LATENCY - which verdict to lead with
    FailureView failure,
    LatencyView latency,
    List<GraphNode> nodes,
    List<GraphEdge> edges,
    List<TimelineBar> timeline,
    long totalDuration) {
}
