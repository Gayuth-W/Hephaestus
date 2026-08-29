package com.gayuth.hephaestus.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.gayuth.hephaestus.dto.AnalyzeResponse;
import com.gayuth.hephaestus.dto.ParsedTraceDTO;
import com.gayuth.hephaestus.enums.Mode;
import com.gayuth.hephaestus.graph.TraceGraphBuilder;
import com.gayuth.hephaestus.ingest.TraceParser;
import com.gayuth.hephaestus.model.TraceGraph;
import com.gayuth.hephaestus.service.AnalysisService;
import com.gayuth.hephaestus.service.ReportService;

/**
 * Analyzes a trace and persists the result against the authenticated user.
 * Accepts either {@code {"type": "...", "trace": {...}}} or a bare trace body.
 *
 * <p>
 * The parser is now handed the {@link JsonNode} directly. The old code called
 * {@code parser.parse(traceNode.toString())}, serializing an already-parsed
 * tree back to a string purely so the parser could re-parse it. The raw string
 * is still produced once, for storage.
 *
 * <p>
 * The three collaborators are plain {@code new} rather than beans on purpose:
 * all are stateless, and keeping them out of the container is what lets the
 * analysis core stay framework-free and unit-testable by direct execution.
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

        ParsedTraceDTO parsed = parser.parse(traceNode);
        TraceGraph graph = builder.build(parsed.traceId(), parsed.spans());
        AnalyzeResponse response = analysis.analyze(graph, Mode.from(type));

        reports.save(principal.getName(), parsed.traceId(), traceNode.toString(), response);
        return response;
    }
}
