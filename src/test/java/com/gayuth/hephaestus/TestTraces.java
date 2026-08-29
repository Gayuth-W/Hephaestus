package com.gayuth.hephaestus;

import com.gayuth.hephaestus.model.TraceGraph;
import com.gayuth.hephaestus.ingest.TraceParser;
import com.gayuth.hephaestus.graph.TraceGraphBuilder;
import com.gayuth.hephaestus.dto.ParsedTraceDTO;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class TestTraces {
    public static TraceGraph graph(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get("src/test/resources/traces/" + filename)));
            TraceParser parser = new TraceParser();
            ParsedTraceDTO parsed = parser.parse(content);
            TraceGraphBuilder builder = new TraceGraphBuilder();
            return builder.build(parsed.traceId(), parsed.spans());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
