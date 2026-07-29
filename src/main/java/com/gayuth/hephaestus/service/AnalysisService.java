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

}
