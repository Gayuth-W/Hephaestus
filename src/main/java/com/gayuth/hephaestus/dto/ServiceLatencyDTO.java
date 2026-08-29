package com.gayuth.hephaestus.dto;

/**
 * One service's latency contribution, measured two ways.
 *
 * <p>
 * {@code selfTime} is total exclusive time across every span of this service;
 * {@code workShare} is its fraction of ALL self time in the trace, so shares
 * sum to 100%. This measures work done, not delay caused - concurrent work
 * inflates it.
 *
 * <p>
 * {@code criticalPathTime} is the self time this service spent on the critical
 * path only, and {@code criticalShare} its fraction of the critical path. This
 * is the number that answers "what made the request slow": a service that is
 * busy but fully overlapped by other work scores 0 here.
 */
public record ServiceLatencyDTO(
        String service,
        long selfTime,
        double workShare,
        long criticalPathTime,
        double criticalShare) {
}
