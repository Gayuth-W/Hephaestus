package com.gayuth.hephaestus.dto;

import java.util.List;

public record LatencyView(String sink, double contribution, String confidence, String reason,
        List<ServiceSelf> breakdown) {
}
