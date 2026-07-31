package com.gayuth.hephaestus.persistence;

import java.time.Instant;
import java.util.UUID;

/** Lightweight row for the history list (no JSON payload). */
public record ReportSummary(UUID id, String traceId, String mode, Instant createdAt) {
}
