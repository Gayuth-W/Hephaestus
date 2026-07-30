package com.gayuth.hephaestus.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.gayuth.hephaestus.graph.TraceGraphBuilder;
import com.gayuth.hephaestus.dto.ParsedTraceDTO;
import com.gayuth.hephaestus.ingest.TraceParser;
import com.gayuth.hephaestus.model.TraceGraph;
import com.gayuth.hephaestus.dto.AnalyzeResponse;
import com.gayuth.hephaestus.service.AnalysisService;
import com.gayuth.hephaestus.enums.Mode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin HTTP shim over {@link AnalysisService}. Accepts either
 * {@code {"type": "...", "trace": {...}}} or a bare trace body.
 */
@RestController
@RequestMapping("/api/traces")
public class TraceController {

    private final TraceParser parser = new TraceParser();
    private final TraceGraphBuilder builder = new TraceGraphBuilder();
    private final AnalysisService analysis = new AnalysisService();

    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@RequestBody JsonNode body) {
        JsonNode traceNode = body.has("trace") ? body.get("trace") : body;
        String type = body.hasNonNull("type") ? body.get("type").asText() : "AUTO";

        ParsedTraceDTO parsed = parser.parse(traceNode.toString());
        TraceGraph graph = builder.build(parsed.traceId(), parsed.spans());
        return analysis.analyze(graph, Mode.from(type));
    }
}
