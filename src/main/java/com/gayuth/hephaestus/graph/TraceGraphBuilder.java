package com.gayuth.hephaestus.graph;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gayuth.hephaestus.dto.SpanDTO;
import com.gayuth.hephaestus.exception.InvalidTraceException;
import com.gayuth.hephaestus.model.SpanNode;
import com.gayuth.hephaestus.model.TraceGraph;

/**
 * Builds the call tree from a flat span list.
 *   Pass 1 - validate and index every span by id            O(n)
 *   Pass 2 - attach each span to its parent                 O(n)
 *   Pass 3 - prove reachability and bound depth, iteratively O(n)
 *
 * <p>
 * Pass 3 is the one that matters. Checking only for dangling parents lets a
 * parent cycle through: a cycle can never contain the root (the root is the
 * span with no parent), so it always lands in a component the traversal never
 * visits. The old builder accepted such a trace, reported the full span count,
 * analysed only the reachable half, and answered "no failing spans" while
 * ERROR spans sat unreachable in the payload.
 *
 * <p>
 * The size and depth caps exist because every downstream traversal - RCA,
 * latency, response assembly, and {@code SpanNode.sortChildren} itself - is
 * recursive. Without a bound, a deep trace is a StackOverflowError surfacing as
 * a 500. Rejecting oversized input here is one check that protects eight
 * traversals; rewriting them all iteratively would be far more code for the
 * same guarantee. Note the depth check runs BEFORE sortChildren, which is
 * itself recursive.
 */
public final class TraceGraphBuilder {

  /** Well above any realistic single request; low enough to bound memory. */
  public static final int MAX_SPANS = 50_000;

  /** Real call stacks are tens deep. A thousand is already pathological. */
  public static final int MAX_DEPTH = 1_000;

  public TraceGraph build(String traceId, List<SpanDTO> spans) {
    if (spans == null || spans.isEmpty()) {
      throw new InvalidTraceException("trace has no spans");
    }
    if (spans.size() > MAX_SPANS) {
      throw new InvalidTraceException(
          "trace has " + spans.size() + " spans, which exceeds the limit of " + MAX_SPANS);
    }

    // Pass 1: validate and index.
    Map<String, SpanNode> index = new HashMap<>(spans.size() * 2);
    for (SpanDTO span : spans) {
      if (span.spanId() == null || span.spanId().isBlank()) {
        throw new InvalidTraceException("a span is missing a spanId (service: " + span.serviceName() + ")");
      }
      if (span.serviceName() == null || span.serviceName().isBlank()) {
        // Without this, a null name propagates all the way into the verdict and
        // the UI renders "null was the deepest failing dependency".
        throw new InvalidTraceException("span '" + span.spanId() + "' is missing a serviceName");
      }
      if (span.duration() < 0) {
        throw new InvalidTraceException(
            "span '" + span.spanId() + "' has a negative duration: " + span.duration());
      }
      if (index.putIfAbsent(span.spanId(), new SpanNode(span)) != null) {
        throw new InvalidTraceException("duplicate spanId: " + span.spanId());
      }
    }

    // Pass 2: link to parents, locate the single root.
    SpanNode root = null;
    for (SpanNode node : index.values()) {
      String parentId = node.span().parentSpanId();
      if (parentId == null || parentId.isBlank()) {
        if (root != null) {
          throw new InvalidTraceException(
              "trace has more than one root span (" + root.spanId() + ", " + node.spanId() + ")");
        }
        root = node;
        continue;
      }
      SpanNode parent = index.get(parentId);
      if (parent == null) {
        throw new InvalidTraceException(
            "span '" + node.serviceName() + "' references unknown parent " + parentId);
      }
      // Partial overlap is tolerated - real traces carry clock skew and the
      // latency engine clamps child windows. Fully disjoint is not skew, it is
      // a broken trace, and silently accepting it inflates the parent's self
      // time by the whole child duration.
      if (node.span().startTime() > parent.span().endTime()
          || node.span().endTime() < parent.span().startTime()) {
        throw new InvalidTraceException("span '" + node.spanId() + "' ("
            + node.span().startTime() + ".." + node.span().endTime()
            + ") lies entirely outside its parent '" + parent.spanId() + "' ("
            + parent.span().startTime() + ".." + parent.span().endTime() + ")");
      }
      parent.addChild(node);
    }

    if (root == null) {
      throw new InvalidTraceException("trace has no root span (every span has a parent - cyclic or detached)");
    }

    // Pass 3: reachability and depth, before any recursive traversal runs.
    verifyShape(root, index.size());

    root.sortChildren();
    return new TraceGraph(traceId, root, index);
  }

  /** Iterative so a pathologically deep trace cannot blow the stack here either. */
  private static void verifyShape(SpanNode root, int total) {
    int seen = 0;
    Deque<SpanNode> nodes = new ArrayDeque<>();
    Deque<Integer> depths = new ArrayDeque<>();
    nodes.push(root);
    depths.push(1);
    while (!nodes.isEmpty()) {
      SpanNode n = nodes.pop();
      int depth = depths.pop();
      seen++;
      if (depth > MAX_DEPTH) {
        throw new InvalidTraceException(
            "trace is nested deeper than " + MAX_DEPTH + " levels at span '" + n.spanId() + "'");
      }
      for (SpanNode c : n.children()) {
        nodes.push(c);
        depths.push(depth + 1);
      }
    }
    if (seen != total) {
      throw new InvalidTraceException("trace has " + (total - seen)
          + " span(s) unreachable from the root - detached subtree or parent cycle");
    }
  }
}
