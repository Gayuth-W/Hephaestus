package com.gayuth.hephaestus.dto;

import java.util.List;

/** Latency verdict as rendered by the UI. */
public record LatencyView(
        String sink,
        double criticalShare,
        String confidence,
        String reason,
        long criticalPathTime,
        List<ServiceSelf> breakdown) {
}
