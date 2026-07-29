package com.gayuth.hephaestus.rca;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.gayuth.hephaestus.dto.RcaResultDTO;
import com.gayuth.hephaestus.enums.Confidence;
import com.gayuth.hephaestus.model.SpanNode;
import com.gayuth.hephaestus.model.TraceGraph;

/**
 * Dependency-aware failure root cause analysis.
 *
 * A failing service whose failure is explained by a failing dependency is a
 * cascade victim, not a cause. A failing node is a ROOT CAUSE iff no node in
 * its subtree below itself is also failing - the deepest failing boundary.
 *
 * All deepest-failing nodes are returned, so two independent failures in a
 * fan-out trace are each reported rather than collapsed into one.
 */
public final class FailureRcaEngine {

    public RcaResultDTO analyze(TraceGraph graph) {
        Set<String> rootCauses = new LinkedHashSet<>();
        Set<String> affected = new LinkedHashSet<>();
        collect(graph.root(), rootCauses, affected);
        affected.removeAll(rootCauses); // a service that is a cause anywhere is not merely a victim

        List<String> causes = new ArrayList<>(rootCauses);
        List<String> victims = new ArrayList<>(affected);

        if (causes.isEmpty()) {
            return new RcaResultDTO(causes, victims,
                    "No failing spans in this trace.", Confidence.HIGH);
        }
        if (causes.size() == 1) {
            return new RcaResultDTO(causes, victims,
                    causes.get(0) + " was the deepest failing dependency; "
                            + "failures above it are cascades.",
                    Confidence.HIGH);
        }
        return new RcaResultDTO(causes, victims,
                causes.size() + " independent failure points ("
                        + String.join(", ", causes) + "); no single origin.",
                Confidence.MEDIUM);
    }

    /**
     * Post-order walk.
     * @return true if the subtree rooted at {@code node} (including it) contains a failing span.
     */
    private boolean collect(SpanNode node, Set<String> rootCauses, Set<String> affected) {
        boolean descendantFails = false;
        for (SpanNode child : node.children()) {
            descendantFails |= collect(child, rootCauses, affected);
        }
        if (node.isError()) {
            if (descendantFails) {
                affected.add(node.serviceName());   // failing, but a dependency also failed
            } else {
                rootCauses.add(node.serviceName());  // deepest failing boundary
            }
            return true;
        }
        return descendantFails;
    }
}