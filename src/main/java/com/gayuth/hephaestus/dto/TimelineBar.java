package com.gayuth.hephaestus.dto;

/**
 * One row of the timeline. {@code status} is what lets the UI colour failing
 * spans - without it every bar renders identically and the timeline is useless
 * for debugging.
 */
public record TimelineBar(
        String spanId,
        String service,
        String status, // OK or ERROR
        long startTime,
        long duration,
        long selfTime,
        boolean rootCause,
        boolean sink,
        boolean onCriticalPath) {
}
