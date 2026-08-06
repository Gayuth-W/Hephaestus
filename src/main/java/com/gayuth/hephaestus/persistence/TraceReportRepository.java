package com.gayuth.hephaestus.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TraceReportRepository extends JpaRepository<TraceReport, UUID> {

    @Query("select new com.gayuth.hephaestus.persistence.ReportSummary(r.id, r.traceId, r.mode, r.createdAt) "
            + "from TraceReport r where r.createdBy = :owner order by r.createdAt desc")
    List<ReportSummary> findSummariesByOwner(@Param("owner") String owner);

    Optional<TraceReport> findByIdAndCreatedBy(UUID id, String createdBy);

    long deleteByIdAndCreatedBy(UUID id, String createdBy);
}
