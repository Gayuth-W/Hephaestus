package com.gayuth.hephaestus.dto;

import java.util.List;

/** Failure verdict as rendered by the UI. */
public record FailureView(
        List<String> rootCauses,
        List<String> rootCauseSpanIds,
        List<String> affectedServices,
        String confidence,
        String reason) {
}
