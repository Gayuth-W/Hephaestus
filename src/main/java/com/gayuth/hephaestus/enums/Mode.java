package com.gayuth.hephaestus.enums;

/**
 * Requested analysis mode. AUTO picks FAILURE when any span errored, else
 * LATENCY.
 */
public enum Mode {
    AUTO,
    FAILURE,
    LATENCY;

    public static Mode from(String raw) {
        if (raw == null)
            return AUTO;
        try {
            return Mode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
