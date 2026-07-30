package com.gayuth.hephaestus.dto;

public record TimelineBar(String spanId, String service, long startTime, long duration, boolean sink) {
}
