package com.gayuth.hephaestus.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TraceReportRepository extends JpaRepository<TraceReport, UUID> {

    @Query("select new com.gayuth.hephaestus.persistence.ReportSummary(r.id, r.traceId, r.mode, r.createdAt) "
            + "from TraceReport r order by r.createdAt desc")
    List<ReportSummary> findAllSummaries();
}
