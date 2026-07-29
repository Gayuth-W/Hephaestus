package com.gayuth.hephaestus.ingest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gayuth.hephaestus.dto.ParsedTraceDTO;
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

  public ParsedTraceDTO parse(String json) {
    try {
      JsonNode root = mapper.readTree(json);
      String traceId = text(root, "traceId", null);

      JsonNode spansNode = root.get("spans");
      if (spansNode == null || !spansNode.isArray()) {
        throw new IllegalArgumentException("trace JSON is missing a 'spans' array");
      }

      List<SpanDTO> spans = new ArrayList<>();
      for (JsonNode s : spansNode) {
        spans.add(new SpanDTO(
          text(s, "spanId", null),
          text(s, "parentSpanId", null),
          text(s, "serviceName", null),
          s.path("startTime").asLong(0L),
          s.path("duration").asLong(0L),
          parseStatus(text(s, "status", "OK")),
          readAttributes(s.get("attributes"))
        ));
      }
      return new ParsedTraceDTO(traceId, spans);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("malformed trace JSON: " + e.getOriginalMessage(), e);
    }
  }

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
