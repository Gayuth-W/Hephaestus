package com.gayuth.hephaestus.dto;

import java.util.List;

/** A parsed trace: its id plus the flat list of spans, ready for graph construction. */
public record ParsedTraceDTO(String traceId, List<SpanDTO> spans) {
  
}
