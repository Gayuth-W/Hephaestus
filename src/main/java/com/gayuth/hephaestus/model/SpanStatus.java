package com.gayuth.hephaestus.model;

/** Span outcome. Extend with more OpenTelemetry status codes if needed. */
public enum SpanStatus {
    OK,
    ERROR;

    public boolean isError() {
        return this == ERROR;
    }
}
