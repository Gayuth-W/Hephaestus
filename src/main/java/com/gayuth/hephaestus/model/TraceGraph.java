package com.gayuth.hephaestus.model;

import java.util.Collections;
import java.util.Map;

/** Result of graph construction: the root span plus a spanId -> node index. */
public final class TraceGraph {

    private final String traceId;
    private final SpanNode root;
    private final Map<String, SpanNode> index;

    public TraceGraph(String traceId, SpanNode root, Map<String, SpanNode> index) {
        this.traceId = traceId;
        this.root = root;
        this.index = Collections.unmodifiableMap(index);
    }

    public String traceId() {
        return traceId;
    }

    public SpanNode root() {
        return root;
    }

    public SpanNode byId(String spanId) {
        return index.get(spanId);
    }

    public int size() {
        return index.size();
    }
}
