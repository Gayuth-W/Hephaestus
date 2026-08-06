package com.gayuth.hephaestus.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.gayuth.hephaestus.persistence.ReportSummary;
import com.gayuth.hephaestus.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/** The signed-in user's own incident history and reports. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    public List<ReportSummary> history(Principal principal) {
        return reports.history(principal.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JsonNode> byId(@PathVariable UUID id, Principal principal) {
        return reports.result(id, principal.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        return reports.delete(id, principal.getName())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
