package com.gayuth.hephaestus.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** One stored trace analysis, owned by the user who created it. */
@Entity
@Table(name = "trace_reports")
public class TraceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trace_id")
    private String traceId;

    private String mode;

    @Column(name = "created_by")
    private String createdBy; // owner's email

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_trace", columnDefinition = "jsonb", nullable = false)
    private String rawTrace;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String result;

    protected TraceReport() {
    } // JPA

    public TraceReport(String traceId, String mode, String createdBy, String rawTrace, String result) {
        this.traceId = traceId;
        this.mode = mode;
        this.createdBy = createdBy;
        this.rawTrace = rawTrace;
        this.result = result;
    }

    public UUID getId() {
        return id;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getMode() {
        return mode;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getRawTrace() {
        return rawTrace;
    }

    public String getResult() {
        return result;
    }
}
