package com.gayuth.hephaestus.dto;

public record GraphNode(
    String spanId,
    String service,
    String status, // OK or ERROR
    long startTime,
    long duration,
    long exclusiveTime, // self time (ms)
    boolean rootCause, // failure origin
    boolean sink // latency sink
) {
}
