package com.gayuth.hephaestus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gayuth.hephaestus.persistence.ReportSummary;
import com.gayuth.hephaestus.persistence.TraceReport;
import com.gayuth.hephaestus.persistence.TraceReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists each analysis against its owner and serves only that owner's
 * reports.
 * The result object is serialized to JSON; mode is read back out of it, so this
 * stays decoupled from the response DTO shape.
 */
@Service
public class ReportService {

    private final TraceReportRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportService(TraceReportRepository repository) {
        this.repository = repository;
    }

    /** Store one analysis owned by {@code owner}; returns the new report id. */
    public UUID save(String owner, String traceId, String rawTraceJson, Object result) {
        String resultJson = write(result);
        String mode = readMode(resultJson);
        return repository.save(new TraceReport(traceId, mode, owner, rawTraceJson, resultJson)).getId();
    }

    public List<ReportSummary> history(String owner) {
        return repository.findSummariesByOwner(owner);
    }

    /** The stored analysis for one of the owner's reports, or empty. */
    public Optional<JsonNode> result(UUID id, String owner) {
        return repository.findByIdAndCreatedBy(id, owner).map(r -> read(r.getResult()));
    }

    /**
     * Delete one of the owner's reports; false if it doesn't exist or isn't theirs.
     */
    @Transactional
    public boolean delete(UUID id, String owner) {
        return repository.deleteByIdAndCreatedBy(id, owner) > 0;
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
