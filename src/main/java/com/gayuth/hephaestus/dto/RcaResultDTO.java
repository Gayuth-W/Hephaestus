package com.gayuth.hephaestus.dto;

import java.util.List;

import com.gayuth.hephaestus.enums.Confidence;

/**
 * Result of failure root cause analysis.
 *
 * <p>{@code rootCauses} may contain more than one service: a trace can have
 * several independent failure origins (separate failing subtrees).
 * {@code affectedServices} are failing services that are NOT root causes -
 * i.e. cascade victims.
 */
public record RcaResultDTO(
        List<String> rootCauses,
        List<String> affectedServices,
        String reason,
        Confidence confidence
) {
    public boolean hasFailure() {
        return !rootCauses.isEmpty();
    }
}
