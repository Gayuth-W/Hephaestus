package com.gayuth.hephaestus.dto;

/**
 * One node of the call tree. Note this is per-SPAN, not per-service: a service
 * called twice in one trace appears twice, which is why the view is labelled a
 * call tree rather than a service dependency graph.
 */
public record GraphNode(
    String spanId,
    String service,
    String status, // OK or ERROR
    long startTime,
    long duration,
    long selfTime,
    boolean rootCause, // this exact span was blamed
    boolean sink, // this span is the latency sink on the critical path
    boolean onCriticalPath) {
}
