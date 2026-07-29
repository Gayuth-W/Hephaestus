package com.gayuth.hephaestus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** A node in the dependency graph: one span plus the spans it called. */
public final class SpanNode {

    private final SpanDTO span;
    private final List<SpanNode> children = new ArrayList<>();

    public SpanNode(SpanDTO span) {
        this.span = span;
    }

    /** Called only by the graph builder during construction. */
    public void addChild(SpanNode child) {
        children.add(child);
    }

    /**
     * Sort children by start time (then spanId) so every traversal is
     * deterministic — important for stable RCA output and readable timelines.
     */
    public void sortChildren() {
        children.sort(Comparator
                .comparingLong((SpanNode n) -> n.span.startTime())
                .thenComparing(n -> n.span.spanId()));
        for (SpanNode c : children) {
            c.sortChildren();
        }
    }

    public SpanDTO span() {
        return span;
    }

    public List<SpanNode> children() {
        return Collections.unmodifiableList(children);
    }

    public String spanId() {
        return span.spanId();
    }

    public String serviceName() {
        return span.serviceName();
    }

    public boolean isError() {
        return span.status().isError();
    }
}
