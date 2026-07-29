package com.gayuth.hephaestus.dto;
import java.util.List;

import com.gayuth.hephaestus.enums.Confidence;

/**
 * Result of latency analysis. {@code breakdown} is every service ranked by
 * exclusive (self) time; {@code latencySink} is the top one. When no service
 * clearly dominates, {@code confidence} is LOW and the sink is only nominal.
 */
public record LatencyResultDTO(String latencySink, double contribution, Confidence confidence, String reason, List<ServiceLatencyDTO> breakdown) {

}
