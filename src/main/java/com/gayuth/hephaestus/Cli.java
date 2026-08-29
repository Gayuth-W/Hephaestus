package com.gayuth.hephaestus;

import com.gayuth.hephaestus.model.TraceGraph;
import com.gayuth.hephaestus.ingest.TraceParser;
import com.gayuth.hephaestus.graph.TraceGraphBuilder;
import com.gayuth.hephaestus.dto.ParsedTraceDTO;
import com.gayuth.hephaestus.dto.AnalyzeResponse;
import com.gayuth.hephaestus.service.AnalysisService;
import com.gayuth.hephaestus.enums.Mode;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class Cli {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println(
                    "Usage: mvn exec:java -Dexec.mainClass=\"com.gayuth.hephaestus.Cli\" -Dexec.args=\"<path-to-json-file>\"");
            System.exit(1);
        }

        String filename = args[0];
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            TraceParser parser = new TraceParser();
            ParsedTraceDTO parsed = parser.parse(content);
            TraceGraphBuilder builder = new TraceGraphBuilder();
            TraceGraph graph = builder.build(parsed.traceId(), parsed.spans());

            AnalysisService analysisService = new AnalysisService();
            AnalyzeResponse response = analysisService.analyze(graph, Mode.AUTO);

            ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
            System.out.println(ow.writeValueAsString(response));
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error processing trace: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
