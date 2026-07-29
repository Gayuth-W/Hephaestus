package com.gayuth.hephaestus.ingest;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gayuth.hephaestus.dto.SpanDTO;
import com.gayuth.hephaestus.model.SpanStatus;

/**
 * Maps trace JSON to domain {@link SpanDTO}s. This is the ONLY place Jackson is
 * used - the model and analysis engines stay dependency-free. The tree API is
 * used so unknown fields are ignored and missing optional fields degrade
 * gracefully rather than throwing.
 */
public final class TraceParser {

  private final ObjectMapper mapper = new ObjectMapper();

  private static SpanStatus parseStatus(String raw) {
    return "ERROR".equalsIgnoreCase(raw == null ? "" : raw.trim()) ? SpanStatus.ERROR : SpanStatus.OK;
  }

  private static String text(JsonNode node, String field, String fallback) {
    JsonNode v = node.get(field);
    return (v == null || v.isNull()) ? fallback : v.asText();
  }

  private static Map<String, Object> readAttributes(JsonNode attrs) {
    if (attrs == null || !attrs.isObject()) {
      return Map.of();
    }
    Map<String, Object> out = new LinkedHashMap<>();
    attrs.fields().forEachRemaining(e -> out.put(e.getKey(), e.getValue().asText()));
    return out;
  }
}
