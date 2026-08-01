package com.gayuth.hephaestus.controller;

import com.gayuth.hephaestus.persistence.ReportSummary;
import com.gayuth.hephaestus.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Read-side: incident history and a single saved report. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    public List<ReportSummary> history() {
        return reports.history();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return reports.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
