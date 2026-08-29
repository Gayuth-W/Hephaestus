package com.gayuth.hephaestus.enums;

import com.gayuth.hephaestus.exception.InvalidTraceException;

/**
 * Requested analysis mode. AUTO decides which verdict the UI leads with:
 * FAILURE when any span errored, else LATENCY. Both analyses always run.
 *
 * <p>
 * An unrecognised value is rejected rather than silently coerced to AUTO. The
 * old behaviour meant a typo like "LATNECY" quietly returned a different
 * analysis than the caller asked for, with nothing in the response to say so.
 */
public enum Mode {
    AUTO,
    FAILURE,
    LATENCY;

    public static Mode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        try {
            return Mode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidTraceException(
                    "unknown mode '" + raw + "' - expected AUTO, FAILURE or LATENCY");
        }
    }
}
