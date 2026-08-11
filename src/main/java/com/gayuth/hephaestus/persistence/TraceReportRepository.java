package com.gayuth.hephaestus.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TraceReportRepository extends JpaRepository<TraceReport, UUID> {

    /**
     * History for one owner, newest first, bounded by {@code page}.
     *
     * <p>
     * This used to be unbounded: every report the user had ever created was
     * loaded and returned on every page view of the sidebar, forever. The
     * {@code (created_by, created_at desc)} index added in V3 serves the filter
     * and the sort together.
     */
    @Query("select new com.gayuth.hephaestus.persistence.ReportSummary(r.id, r.traceId, r.mode, r.createdAt) "
            + "from TraceReport r where r.createdBy = :owner order by r.createdAt desc")
    List<ReportSummary> findSummariesByOwner(@Param("owner") String owner, Pageable page);

    Optional<TraceReport> findByIdAndCreatedBy(UUID id, String createdBy);

    long deleteByIdAndCreatedBy(UUID id, String createdBy);
}
