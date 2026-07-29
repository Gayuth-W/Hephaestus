package com.gayuth.hephaestus.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gayuth.hephaestus.dto.SpanDTO;
import com.gayuth.hephaestus.exception.InvalidTraceException;
import com.gayuth.hephaestus.model.SpanNode;
import com.gayuth.hephaestus.model.TraceGraph;

/**
 * Builds a dependency graph from a flat span list using the two-pass approach:
 *   Pass 1 - index every span by id            O(n)
 *   Pass 2 - attach each span to its parent     O(n)
 * Validation is strict: a missing id, a duplicate id, an unknown parent, or the
 * absence of exactly one root span is rejected rather than silently tolerated.
 */
public final class TraceGraphBuilder {

  public TraceGraph build(String traceId, List<SpanDTO> spans) {
    if (spans == null || spans.isEmpty()) {
      throw new InvalidTraceException("trace has no spans");
    }

    // Pass 1: index spans by id.
    Map<String, SpanNode> index = new HashMap<>(spans.size() * 2);
    for (SpanDTO span : spans) {
      if (span.spanId() == null || span.spanId().isBlank()) {
        throw new InvalidTraceException("span for service '" + span.serviceName() + "' is missing a spanId");
      }
      if (index.putIfAbsent(span.spanId(), new SpanNode(span)) != null) {
        throw new InvalidTraceException("duplicate spanId: " + span.spanId());
      }
    }

    // Pass 2: link each span to its parent and locate the single root.
    SpanNode root = null;
    for (SpanNode node : index.values()) {
      String parentId = node.span().parentSpanId();
      if (parentId == null || parentId.isBlank()) {
        if (root != null) {
          throw new InvalidTraceException("trace has more than one root span ("+ root.spanId() + ", " + node.spanId() + ")");
        }
        root = node;
      } else {
        SpanNode parent = index.get(parentId);
        if (parent == null) {
          throw new InvalidTraceException("span '" + node.serviceName() + "' references unknown parent " + parentId);
        }
        parent.addChild(node);
    }
    }

    if (root == null) {
      throw new InvalidTraceException("trace has no root span (every span has a parent - cyclic or detached)");
    }

    root.sortChildren();
    return new TraceGraph(traceId, root, index);
  }
}
