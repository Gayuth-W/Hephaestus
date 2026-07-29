package com.gayuth.hephaestus.service;

import com.gayuth.hephaestus.latency.LatencyAnalysisEngine;
import com.gayuth.hephaestus.dto.LatencyResultDTO;
import com.gayuth.hephaestus.dto.ServiceLatencyDTO;
import com.gayuth.hephaestus.model.SpanNode;
import com.gayuth.hephaestus.model.TraceGraph;
import com.gayuth.hephaestus.rca.FailureRcaEngine;
import com.gayuth.hephaestus.dto.RcaResultDTO;
import com.gayuth.hephaestus.dto.AnalyzeResponse;
import com.gayuth.hephaestus.dto.FailureView;
import com.gayuth.hephaestus.dto.GraphEdge;
import com.gayuth.hephaestus.dto.GraphNode;
import com.gayuth.hephaestus.dto.LatencyView;
import com.gayuth.hephaestus.dto.ServiceSelf;
import com.gayuth.hephaestus.dto.TimelineBar;
import com.gayuth.hephaestus.enums.Mode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates the analysis engines and assembles the UI response (verdict +
 * graph + timeline). Pure Java - no framework - so it is unit-testable and was
 * verified by direct execution. The Spring controller is a thin shim over this.
 */
public final class AnalysisService {

    private final FailureRcaEngine failureEngine = new FailureRcaEngine();
    private final LatencyAnalysisEngine latencyEngine = new LatencyAnalysisEngine();

    public AnalyzeResponse analyze(TraceGraph graph, Mode requested) {
        Mode effective = (requested == Mode.AUTO)
                ? (anyError(graph.root()) ? Mode.FAILURE : Mode.LATENCY)
                : requested;

        FailureView failureView = null;
        LatencyView latencyView = null;
        Set<String> rootCauseServices = new HashSet<>();
        String sinkService = null;

        if (effective == Mode.FAILURE) {
            RcaResultDTO r = failureEngine.analyze(graph);
            rootCauseServices.addAll(r.rootCauses());
            failureView = new FailureView(r.rootCauses(), r.affectedServices(),
                    r.confidence().name(), r.reason());
        } else {
            LatencyResultDTO r = latencyEngine.analyze(graph);
            sinkService = r.latencySink();
            List<ServiceSelf> breakdown = new ArrayList<>();
            for (ServiceLatencyDTO s : r.breakdown()) {
                breakdown.add(new ServiceSelf(s.service(), s.exclusiveTime(), s.contribution()));
            }
            latencyView = new LatencyView(r.latencySink(), r.contribution(),
                    r.confidence().name(), r.reason(), breakdown);
        }

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        List<TimelineBar> timeline = new ArrayList<>();
        walk(graph.root(), rootCauseServices, sinkService, nodes, edges, timeline);

        long total = Math.max(graph.root().span().duration(),
                maxEnd(graph.root()) - graph.root().span().startTime());

        return new AnalyzeResponse(graph.traceId(), effective.name(),
                failureView, latencyView, nodes, edges, timeline, total);
    }

    private void walk(SpanNode node, Set<String> rootCauses, String sink,
            List<GraphNode> nodes, List<GraphEdge> edges, List<TimelineBar> timeline) {
        boolean isRootCause = rootCauses.contains(node.serviceName());
        boolean isSink = sink != null && sink.equals(node.serviceName());
        long self = LatencyAnalysisEngine.exclusiveTime(node);

        nodes.add(new GraphNode(node.spanId(), node.serviceName(), node.span().status().name(),
                node.span().startTime(), node.span().duration(), self, isRootCause, isSink));
        timeline.add(new TimelineBar(node.spanId(), node.serviceName(),
                node.span().startTime(), node.span().duration(), isSink));

        for (SpanNode child : node.children()) {
            edges.add(new GraphEdge(node.spanId(), child.spanId()));
            walk(child, rootCauses, sink, nodes, edges, timeline);
        }
    }

    private boolean anyError(SpanNode node) {
        if (node.isError())
            return true;
        for (SpanNode c : node.children()) {
            if (anyError(c))
                return true;
        }
        return false;
    }

    private long maxEnd(SpanNode node) {
        long m = node.span().endTime();
        for (SpanNode c : node.children()) {
            m = Math.max(m, maxEnd(c));
        }
        return m;
    }
}
