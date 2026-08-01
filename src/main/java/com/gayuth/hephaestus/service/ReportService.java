package com.gayuth.hephaestus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gayuth.hephaestus.persistence.TraceReportRepository;
import org.springframework.stereotype.Service;

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
}
