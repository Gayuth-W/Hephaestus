package com.gayuth.hephaestus.ingest;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gayuth.hephaestus.dto.SpanDTO;

/**
 * Maps trace JSON to domain {@link SpanDTO}s. This is the ONLY place Jackson is
 * used - the model and analysis engines stay dependency-free. The tree API is
 * used so unknown fields are ignored and missing optional fields degrade
 * gracefully rather than throwing.
 */
public final class TraceParser {

  private final ObjectMapper mapper = new ObjectMapper();


  private static Map<String, Object> readAttributes(JsonNode attrs) {
    if (attrs == null || !attrs.isObject()) {
      return Map.of();
    }
    Map<String, Object> out = new LinkedHashMap<>();
    attrs.fields().forEachRemaining(e -> out.put(e.getKey(), e.getValue().asText()));
    return out;
  }
}
