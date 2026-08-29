package com.gayuth.hephaestus.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gayuth.hephaestus.dto.AnalyzeResponse;
import com.gayuth.hephaestus.dto.FailureView;
import com.gayuth.hephaestus.dto.GraphEdge;
import com.gayuth.hephaestus.dto.GraphNode;
import com.gayuth.hephaestus.dto.LatencyResultDTO;
import com.gayuth.hephaestus.dto.LatencyView;
import com.gayuth.hephaestus.dto.RcaResultDTO;
import com.gayuth.hephaestus.dto.ServiceLatencyDTO;
import com.gayuth.hephaestus.dto.ServiceSelf;
import com.gayuth.hephaestus.dto.TimelineBar;
import com.gayuth.hephaestus.enums.Mode;
import com.gayuth.hephaestus.latency.LatencyAnalysisEngine;
import com.gayuth.hephaestus.model.SpanNode;
import com.gayuth.hephaestus.model.TraceGraph;
import com.gayuth.hephaestus.rca.FailureRcaEngine;

/**
 * Orchestrates the analysis engines and assembles the UI response.
 *
 * <p>
 * Two changes here matter more than anything in the engines:
 *
 * <p>
 * 1. Both engines always run. The old AUTO branch was an if/else - any trace
 * containing an error ran failure analysis ONLY, so {@code latencyView} came
 * back null and the entire latency half of the product was unreachable on
 * exactly the traces people analyse. The latency engine already had the answer;
 * it was being computed and discarded. {@code mode} is now just which verdict
 * the UI leads with.
 *
 * <p>
 * 2. Highlighting is keyed on span id, not service name. Matching
 * {@code rootCauses.contains(serviceName)} painted every span of a service as a
 * failure origin, including healthy ones - a shared database that fails under
 * one branch and succeeds under another was rendered as the cause in both.
 */
public final class AnalysisService {

    private final FailureRcaEngine failureEngine = new FailureRcaEngine();
    private final LatencyAnalysisEngine latencyEngine = new LatencyAnalysisEngine();

    public AnalyzeResponse analyze(TraceGraph graph, Mode requested) {
        Mode lead = (requested == Mode.AUTO)
                ? (anyError(graph.root()) ? Mode.FAILURE : Mode.LATENCY)
                : requested;

        RcaResultDTO rca = failureEngine.analyze(graph);
        LatencyResultDTO latency = latencyEngine.analyze(graph);

        FailureView failureView = new FailureView(rca.rootCauses(), rca.rootCauseSpanIds(),
                rca.affectedServices(), rca.confidence().name(), rca.reason());

        List<ServiceSelf> breakdown = new ArrayList<>();
        for (ServiceLatencyDTO s : latency.breakdown()) {
            breakdown.add(new ServiceSelf(s.service(), s.selfTime(), s.workShare(),
                    s.criticalPathTime(), s.criticalShare()));
        }
        LatencyView latencyView = new LatencyView(latency.latencySink(), latency.criticalShare(),
                latency.confidence().name(), latency.reason(), latency.criticalPathTime(), breakdown);

        Set<String> rootCauseSpanIds = new HashSet<>(rca.rootCauseSpanIds());
        Set<String> criticalSpanIds = new HashSet<>(latency.criticalPathSpanIds());

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        List<TimelineBar> timeline = new ArrayList<>();
        walk(graph.root(), rootCauseSpanIds, criticalSpanIds, latency.latencySink(),
                nodes, edges, timeline);

        long total = Math.max(graph.root().span().duration(),
                maxEnd(graph.root()) - graph.root().span().startTime());

        return new AnalyzeResponse(graph.traceId(), lead.name(), failureView, latencyView,
                nodes, edges, timeline, total);
    }

    private void walk(SpanNode node, Set<String> rootCauseSpanIds, Set<String> criticalSpanIds,
            String sinkService, List<GraphNode> nodes, List<GraphEdge> edges,
            List<TimelineBar> timeline) {
        boolean isRootCause = rootCauseSpanIds.contains(node.spanId());
        boolean onCriticalPath = criticalSpanIds.contains(node.spanId());
        // The sink badge belongs to the sink service's spans that actually sit on
        // the critical path - not to every span that happens to share the name.
        boolean isSink = onCriticalPath && node.serviceName().equals(sinkService);
        long self = LatencyAnalysisEngine.exclusiveTime(node);
        String status = node.span().status().name();

        nodes.add(new GraphNode(node.spanId(), node.serviceName(), status,
                node.span().startTime(), node.span().duration(), self,
                isRootCause, isSink, onCriticalPath));
        timeline.add(new TimelineBar(node.spanId(), node.serviceName(), status,
                node.span().startTime(), node.span().duration(), self,
                isRootCause, isSink, onCriticalPath));

        for (SpanNode child : node.children()) {
            edges.add(new GraphEdge(node.spanId(), child.spanId()));
            walk(child, rootCauseSpanIds, criticalSpanIds, sinkService, nodes, edges, timeline);
        }
    }

    private boolean anyError(SpanNode node) {
        if (node.isError()) {
            return true;
        }
        for (SpanNode c : node.children()) {
            if (anyError(c)) {
                return true;
            }
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
