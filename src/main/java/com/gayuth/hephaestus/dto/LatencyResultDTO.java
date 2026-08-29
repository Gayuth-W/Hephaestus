package com.gayuth.hephaestus.dto;

import java.util.List;

import com.gayuth.hephaestus.enums.Confidence;

/**
 * Result of latency analysis. {@code latencySink} is the service holding the
 * largest share of the critical path - not merely the busiest service.
 * {@code criticalPathSpanIds} is the ordered chain of spans that determined
 * when the request finished.
 */
public record LatencyResultDTO(
        String latencySink,
        double criticalShare,
        Confidence confidence,
        String reason,
        long criticalPathTime,
        List<String> criticalPathSpanIds,
        List<ServiceLatencyDTO> breakdown) {
}
