package com.gayuth.hephaestus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gayuth.hephaestus.persistence.ReportSummary;
import com.gayuth.hephaestus.persistence.TraceReport;
import com.gayuth.hephaestus.persistence.TraceReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists each analysis and serves saved reports + history. Decoupled from the
 * response DTO shape: the result object is serialized to JSON, and the mode is
 * read back out of that JSON rather than off a typed accessor.
 */
@Service
public class ReportService {

    private final TraceReportRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportService(TraceReportRepository repository) {
        this.repository = repository;
    }

    /** Store one analysis; returns the new report id. */
    public UUID save(String traceId, String rawTraceJson, Object result) {
        String resultJson = write(result);
        String mode = readMode(resultJson);
        return repository.save(new TraceReport(traceId, mode, rawTraceJson, resultJson)).getId();
    }

    /** Delete a report; returns false if it didn't exist. */
    public boolean delete(UUID id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    public List<ReportSummary> history() {
        return repository.findAllSummaries();
    }

    /** The stored analysis JSON for a report, or empty if the id is unknown. */
    public Optional<JsonNode> result(UUID id) {
        return repository.findById(id).map(r -> read(r.getResult()));
    }

    private String write(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize analysis result", e);
        }
    }

    private JsonNode read(String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("stored result is not valid JSON", e);
        }
    }

    private String readMode(String resultJson) {
        JsonNode n = read(resultJson).get("mode");
        return (n == null || n.isNull()) ? null : n.asText();
    }
}
