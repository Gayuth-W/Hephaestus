package com.gayuth.hephaestus.dto;

/** A service's exclusive (self) time and its share of total request time. */
public record ServiceLatencyDTO(String service, long exclusiveTime, double contribution) {

}
