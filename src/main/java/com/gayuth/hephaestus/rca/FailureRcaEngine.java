package com.gayuth.hephaestus.rca;

import com.gayuth.hephaestus.model.SpanNode;
import com.gayuth.hephaestus.model.TraceGraph;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public RcaResult analyze(TraceGraph graph) {
        Set<String> rootCauses = new LinkedHashSet<>();
        Set<String> affected = new LinkedHashSet<>();
        collect(graph.root(), rootCauses, affected);
        affected.removeAll(rootCauses); // a service that is a cause anywhere is not merely a victim

        List<String> causes = new ArrayList<>(rootCauses);
        List<String> victims = new ArrayList<>(affected);

        if (causes.isEmpty()) {
            return new RcaResult(causes, victims,
                    "No failing spans in this trace.", Confidence.HIGH);
        }
        if (causes.size() == 1) {
            return new RcaResult(causes, victims,
                    causes.get(0) + " was the deepest failing dependency; "
                            + "failures above it are cascades.",
                    Confidence.HIGH);
        }
        return new RcaResult(causes, victims,
                causes.size() + " independent failure points ("
                        + String.join(", ", causes) + "); no single origin.",
                Confidence.MEDIUM);
    }
}