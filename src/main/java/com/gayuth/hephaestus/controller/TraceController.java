package com.gayuth.hephaestus.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.gayuth.hephaestus.dto.AnalyzeResponse;
import com.gayuth.hephaestus.dto.ParsedTraceDTO;
import com.gayuth.hephaestus.enums.Mode;
import com.gayuth.hephaestus.graph.TraceGraphBuilder;
import com.gayuth.hephaestus.ingest.TraceParser;
import com.gayuth.hephaestus.model.TraceGraph;
import com.gayuth.hephaestus.service.AnalysisService;
import com.gayuth.hephaestus.service.ReportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Analyzes a trace and persists the result against the authenticated user.
 * Accepts either {@code {"type": "...", "trace": {...}}} or a bare trace body.
 */
@RestController
@RequestMapping("/api/traces")
public class TraceController {

    private final TraceParser parser = new TraceParser();
    private final TraceGraphBuilder builder = new TraceGraphBuilder();
    private final AnalysisService analysis = new AnalysisService();
    private final ReportService reports;

    public TraceController(ReportService reports) {
        this.reports = reports;
    }

    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@RequestBody JsonNode body, Principal principal) {
        JsonNode traceNode = body.has("trace") ? body.get("trace") : body;
        String type = body.hasNonNull("type") ? body.get("type").asText() : "AUTO";

        ParsedTraceDTO parsed = parser.parse(traceNode.toString());
        TraceGraph graph = builder.build(parsed.traceId(), parsed.spans());
        AnalyzeResponse response = analysis.analyze(graph, Mode.from(type));

        reports.save(principal.getName(), parsed.traceId(), traceNode.toString(), response);
        return response;
    }
}
