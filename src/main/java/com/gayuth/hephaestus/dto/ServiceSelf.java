package com.gayuth.hephaestus.dto;

/** UI projection of {@link ServiceLatencyDTO}. */
public record ServiceSelf(
        String service,
        long selfTime,
        double workShare,
        long criticalPathTime,
        double criticalShare) {
}
