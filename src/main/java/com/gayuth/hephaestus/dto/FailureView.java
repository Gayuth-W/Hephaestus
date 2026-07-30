package com.gayuth.hephaestus.dto;

import java.util.List;

public record FailureView(List<String> rootCauses, List<String> affectedServices, String confidence, String reason) {

}
