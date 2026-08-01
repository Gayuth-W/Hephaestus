package com.gayuth.hephaestus.controller;

import com.gayuth.hephaestus.service.ReportService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-side: incident history and a single saved report. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }
}
