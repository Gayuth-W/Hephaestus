package com.gayuth.hephaestus.dto;

import java.util.List;

import com.gayuth.hephaestus.enums.Confidence;

/**
 * Result of failure root cause analysis.
 *
 * <p>
 * {@code rootCauses} are service names; {@code rootCauseSpanIds} are the exact
 * spans that were blamed. Callers MUST highlight by span id, not by service
 * name - the same service can fail in one branch of a trace and be perfectly
 * healthy in another, and matching on the name paints the healthy span red.
 *
 * <p>
 * {@code affectedServices} are failing services that are NOT origins: cascade
 * victims above a failing dependency, plus leaves demoted to sequential
 * consequences of an earlier sibling failure.
 */
public record RcaResultDTO(
        List<String> rootCauses,
        List<String> rootCauseSpanIds,
        List<String> affectedServices,
        String reason,
        Confidence confidence) {

    public boolean hasFailure() {
        return !rootCauses.isEmpty();
    }
}
