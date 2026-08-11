package com.gayuth.hephaestus.rca;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import com.gayuth.hephaestus.dto.RcaResultDTO;
import com.gayuth.hephaestus.enums.Confidence;
import com.gayuth.hephaestus.model.SpanNode;
import com.gayuth.hephaestus.model.TraceGraph;

/**
 * Dependency- and sequence-aware failure root cause analysis.
 *
 * <p>
 * Step 1 (unchanged): a failing span whose subtree also fails is a cascade
 * victim. The deepest failing spans are the candidates.
 *
 * <p>
 * Step 2 (new): demote candidates that are sequential consequences. Parent to
 * child is not the only way causality flows - siblings invoked in order by the
 * same parent form a chain the span tree does not encode. When a leaf starts
 * moments after an earlier sibling's subtree failed, and fails far faster than
 * that sibling ran, it is downstream of the failure, not a second origin. This
 * is the "inventory reservation failed, so order creation was rejected"
 * shape, and the old engine reported it as an independent root cause.
 *
 * <p>
 * Step 3 (new): group surviving candidates by service. The same dependency
 * failing twice under the same caller is a retry, not two failures - and
 * escalating latency across attempts is evidence of a degrading dependency,
 * which should raise confidence rather than split it.
 */
public final class FailureRcaEngine {

    /**
     * How soon after an earlier sibling's failure a leaf must start to count as
     * its consequence. Wide enough for a retry/backoff hop, tight enough that
     * genuinely independent work does not get swallowed.
     */
    static final long SEQUENTIAL_GAP_MS = 50L;

    /**
     * A consequence fails fast - it is rejected by its caller, it does not do
     * real work. A leaf lasting a meaningful fraction of the sibling that
     * preceded it is doing its own thing and stays an origin.
     */
    static final double FAST_FAIL_RATIO = 0.25d;

    public RcaResultDTO analyze(TraceGraph graph) {
        // All state is local: a single engine instance is shared across requests.
        Map<String, Boolean> subtreeFails = new HashMap<>();
        Map<String, Long> subtreeEnd = new HashMap<>();
        List<SpanNode> deepest = new ArrayList<>();
        index(graph.root(), subtreeFails, subtreeEnd, deepest);

        Set<String> deepestIds = new HashSet<>();
        for (SpanNode n : deepest) {
            deepestIds.add(n.spanId());
        }

        Set<String> demotedIds = new HashSet<>();
        Set<String> demotedServices = new LinkedHashSet<>();
        demoteSequentialConsequences(graph.root(), deepestIds, subtreeFails, subtreeEnd,
                demotedIds, demotedServices);

        List<SpanNode> causes = new ArrayList<>();
        Set<String> affected = new LinkedHashSet<>();
        classify(graph.root(), deepestIds, demotedIds, causes, affected);

        Map<String, List<SpanNode>> byService = new LinkedHashMap<>();
        for (SpanNode n : causes) {
            byService.computeIfAbsent(n.serviceName(), k -> new ArrayList<>()).add(n);
        }
        affected.removeAll(byService.keySet()); // a cause anywhere is not merely a victim

        List<String> causeServices = new ArrayList<>(byService.keySet());
        List<String> causeSpanIds = new ArrayList<>();
        for (SpanNode n : causes) {
            causeSpanIds.add(n.spanId());
        }
        List<String> victims = new ArrayList<>(affected);

        if (causeServices.isEmpty()) {
            return new RcaResultDTO(causeServices, causeSpanIds, victims,
                    "No failing spans in this trace.", Confidence.HIGH);
        }

        String reason;
        Confidence confidence;
        if (causeServices.size() == 1) {
            String service = causeServices.get(0);
            List<SpanNode> attempts = byService.get(service);
            if (attempts.size() == 1) {
                reason = service + " was the deepest failing dependency; failures above it are cascades.";
            } else {
                reason = service + " was the deepest failing dependency, failing on "
                        + attempts.size() + " attempts (" + durations(attempts) + ")"
                        + (escalating(attempts)
                                ? "; latency grew with each attempt, consistent with a degrading dependency"
                                : "")
                        + ". Failures above it are cascades.";
            }
            confidence = Confidence.HIGH;
        } else {
            reason = causeServices.size() + " independent failure points ("
                    + String.join(", ", causeServices) + "); no single origin.";
            confidence = Confidence.MEDIUM;
        }

        String detail = errorDetail(causes);
        if (!detail.isEmpty()) {
            reason += " " + detail;
        }

        if (!demotedServices.isEmpty()) {
            reason += " " + String.join(", ", demotedServices)
                    + " failed immediately after an upstream sibling failure and is reported as a"
                    + " downstream consequence, not an origin.";
        }
        return new RcaResultDTO(causeServices, causeSpanIds, victims, reason, confidence);
    }

    /**
     * Post-order pass recording, for every span: whether its subtree contains a
     * failure, when its subtree actually finishes (children can outlive their
     * parent under skew), and which spans are deepest-failing candidates.
     */
    private boolean index(SpanNode node, Map<String, Boolean> subtreeFails,
            Map<String, Long> subtreeEnd, List<SpanNode> deepest) {
        boolean descendantFails = false;
        long end = node.span().endTime();
        for (SpanNode child : node.children()) {
            descendantFails |= index(child, subtreeFails, subtreeEnd, deepest);
            end = Math.max(end, subtreeEnd.get(child.spanId()));
        }
        subtreeEnd.put(node.spanId(), end);
        boolean fails = node.isError() || descendantFails;
        subtreeFails.put(node.spanId(), fails);
        if (node.isError() && !descendantFails) {
            deepest.add(node);
        }
        return fails;
    }

    /** Children are already sorted by start time, so index order is call order. */
    private void demoteSequentialConsequences(SpanNode node, Set<String> deepestIds,
            Map<String, Boolean> subtreeFails, Map<String, Long> subtreeEnd,
            Set<String> demotedIds, Set<String> demotedServices) {
        List<SpanNode> kids = node.children();
        for (int j = 0; j < kids.size(); j++) {
            SpanNode later = kids.get(j);
            if (!deepestIds.contains(later.spanId())) {
                continue;
            }
            for (int i = 0; i < j; i++) {
                SpanNode earlier = kids.get(i);
                if (!Boolean.TRUE.equals(subtreeFails.get(earlier.spanId()))) {
                    continue;
                }
                long gap = later.span().startTime() - subtreeEnd.get(earlier.spanId());
                boolean followsOn = gap >= 0 && gap <= SEQUENTIAL_GAP_MS;
                boolean failsFast = later.span().duration()
                        <= FAST_FAIL_RATIO * earlier.span().duration();
                if (followsOn && failsFast) {
                    demotedIds.add(later.spanId());
                    demotedServices.add(later.serviceName());
                    break;
                }
            }
        }
        for (SpanNode child : kids) {
            demoteSequentialConsequences(child, deepestIds, subtreeFails, subtreeEnd,
                    demotedIds, demotedServices);
        }
    }

    /** Second post-order pass so causes and victims come out in traversal order. */
    private void classify(SpanNode node, Set<String> deepestIds, Set<String> demotedIds,
            List<SpanNode> causes, Set<String> affected) {
        for (SpanNode child : node.children()) {
            classify(child, deepestIds, demotedIds, causes, affected);
        }
        if (!node.isError()) {
            return;
        }
        if (deepestIds.contains(node.spanId()) && !demotedIds.contains(node.spanId())) {
            causes.add(node);
        } else {
            affected.add(node.serviceName());
        }
    }

    /**
     * Surfaces what the span itself reported about its failure.
     *
     * <p>
     * {@code SpanDTO.attributes} was parsed and then read by nothing. Real
     * OpenTelemetry spans carry the actual error on these keys, so the verdict
     * could say a dependency failed but never how - which is the next question
     * anyone asks. First blamed span that carries any of them wins; absent
     * attributes cost nothing.
     */
    private static String errorDetail(List<SpanNode> causes) {
        for (SpanNode n : causes) {
            Map<String, Object> attrs = n.span().attributes();
            if (attrs.isEmpty()) {
                continue;
            }
            String message = first(attrs, "error.message", "exception.message", "error", "exception.type");
            String status = first(attrs, "http.status_code", "rpc.grpc.status_code", "db.error.code");
            if (message == null && status == null) {
                continue;
            }
            StringBuilder sb = new StringBuilder("Reported by ").append(n.serviceName()).append(": ");
            sb.append(message == null ? "status " + status : message);
            if (message != null && status != null) {
                sb.append(" (status ").append(status).append(")");
            }
            return sb.append('.').toString();
        }
        return "";
    }

    private static String first(Map<String, Object> attrs, String... keys) {
        for (String k : keys) {
            Object v = attrs.get(k);
            if (v != null && !v.toString().isBlank()) {
                return v.toString();
            }
        }
        return null;
    }

    private static String durations(List<SpanNode> attempts) {
        StringJoiner j = new StringJoiner(" then ");
        for (SpanNode n : attempts) {
            j.add(n.span().duration() + "ms");
        }
        return j.toString();
    }

    private static boolean escalating(List<SpanNode> attempts) {
        for (int i = 1; i < attempts.size(); i++) {
            if (attempts.get(i).span().duration() <= attempts.get(i - 1).span().duration()) {
                return false;
            }
        }
        return true;
    }
}
