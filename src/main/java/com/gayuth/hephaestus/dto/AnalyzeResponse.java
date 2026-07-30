package com.gayuth.hephaestus.dto;

import java.util.List;

/**
 * Everything the UI needs to render one analysis: verdict + graph + timeline.
 */
public record AnalyzeResponse(
    String traceId,
    String mode, // FAILURE or LATENCY (the effective mode)
    FailureView failure, // null unless mode == FAILURE
    LatencyView latency, // null unless mode == LATENCY
    List<GraphNode> nodes,
    List<GraphEdge> edges,
    List<TimelineBar> timeline,
    long totalDuration) {
}
