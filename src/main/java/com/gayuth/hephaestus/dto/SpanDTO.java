package com.gayuth.hephaestus.dto;

import java.util.Map;

import com.gayuth.hephaestus.model.SpanStatus;

/**
 * A single parsed span. This is a pure domain type with no serialization
 * annotations, so the whole analysis core compiles and runs without any
 * external dependency. JSON mapping lives in {@code com.trace.ingest.TraceParser}.
 *
 * <p>Times are in milliseconds. {@code parentSpanId} is {@code null} for the
 * root span.
 */
public record SpanDTO(
        String spanId,
        String parentSpanId,
        String serviceName,
        long startTime,
        long duration,
        SpanStatus status,
        Map<String, Object> attributes
) {
    public SpanDTO {
        status = (status == null) ? SpanStatus.OK : status;
        attributes = (attributes == null) ? Map.of() : Map.copyOf(attributes);
    }

    /** End of this span's window, used by the latency engine. */
    public long endTime() {
        return startTime + duration;
    }
}