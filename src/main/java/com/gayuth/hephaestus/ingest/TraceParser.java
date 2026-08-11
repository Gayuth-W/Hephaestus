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
import com.gayuth.hephaestus.exception.InvalidTraceException;
import com.gayuth.hephaestus.model.SpanStatus;

/**
 * Maps trace JSON to domain {@link SpanDTO}s. This is the ONLY place Jackson is
 * used - the model and analysis engines stay dependency-free.
 *
 * <p>
 * Two changes. First, {@link #parse(JsonNode)}: the controller already holds a
 * parsed tree, and the old string-only entry point forced it to serialize that
 * tree back to a string just so this class could parse it again - three copies
 * of a potentially large payload alive at once.
 *
 * <p>
 * Second, timing fields are checked. {@code asLong(0L)} coerced silently, so
 * {@code "duration": "fast"} became a zero-length span and nothing complained.
 * Failures are {@link InvalidTraceException} rather than
 * {@code IllegalArgumentException} so the exception handler can return them to
 * the caller without also exposing every unrelated internal IAE.
 */
public final class TraceParser {

  private final ObjectMapper mapper = new ObjectMapper();

  public ParsedTraceDTO parse(String json) {
    try {
      return parse(mapper.readTree(json));
    } catch (JsonProcessingException e) {
      throw new InvalidTraceException("malformed trace JSON: " + e.getOriginalMessage());
    }
  }

  /** Preferred entry point when the caller already has the tree. */
  public ParsedTraceDTO parse(JsonNode root) {
    if (root == null || !root.isObject()) {
      throw new InvalidTraceException("trace JSON must be an object");
    }
    String traceId = text(root, "traceId", null);

    JsonNode spansNode = root.get("spans");
    if (spansNode == null || !spansNode.isArray()) {
      throw new InvalidTraceException("trace JSON is missing a 'spans' array");
    }

    List<SpanDTO> spans = new ArrayList<>();
    for (JsonNode s : spansNode) {
      String spanId = text(s, "spanId", null);
      spans.add(new SpanDTO(
        spanId,
        text(s, "parentSpanId", null),
        text(s, "serviceName", null),
        number(s, "startTime", spanId),
        number(s, "duration", spanId),
        parseStatus(text(s, "status", "OK")),
        readAttributes(s.get("attributes"))
      ));
    }
    return new ParsedTraceDTO(traceId, spans);
  }

  private static SpanStatus parseStatus(String raw) {
    return "ERROR".equalsIgnoreCase(raw == null ? "" : raw.trim()) ? SpanStatus.ERROR : SpanStatus.OK;
  }

  private static String text(JsonNode node, String field, String fallback) {
    JsonNode v = node.get(field);
    return (v == null || v.isNull()) ? fallback : v.asText();
  }

  private static long number(JsonNode node, String field, String spanId) {
    JsonNode v = node.get(field);
    if (v == null || v.isNull()) {
      return 0L;
    }
    if (!v.isNumber()) {
      throw new InvalidTraceException(
          "span '" + spanId + "' has a non-numeric " + field + ": " + v.asText());
    }
    return v.asLong();
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
