package com.library.analytics.domain.model;

import com.library.analytics.domain.exception.InvalidOperationException;
import com.library.analytics.domain.model.enums.ReportPeriod;
import com.library.analytics.domain.model.enums.ReportStatus;
import com.library.analytics.domain.model.enums.ReportType;
import com.library.shared.domain.model.ReportId;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "analytics_reports", indexes = {
    @Index(name = "idx_report_type", columnList = "report_type"),
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_date", columnList = "report_date")
})
@EntityListeners(AuditingEntityListener.class)
public class AnalyticsReport {

    @EmbeddedId
    private ReportId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_period", nullable = false, length = 20)
    private ReportPeriod reportPeriod;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "generated_by", length = 100)
    private String generatedBy;

    @Column(name = "total_records")
    private Long totalRecords;

    @Column(name = "data_summary", columnDefinition = "TEXT")
    private String dataSummary;

    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportData;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected AnalyticsReport() {
    }

    private AnalyticsReport(ReportId id, ReportType reportType, ReportPeriod reportPeriod,
                            LocalDate reportDate, LocalDate startDate, LocalDate endDate,
                            String generatedBy) {
        this.id = Objects.requireNonNull(id, "Report ID must not be null");
        this.reportType = Objects.requireNonNull(reportType, "Report type must not be null");
        this.reportPeriod = Objects.requireNonNull(reportPeriod, "Report period must not be null");
        this.reportDate = Objects.requireNonNull(reportDate, "Report date must not be null");
        this.startDate = startDate;
        this.endDate = endDate;
        this.generatedBy = generatedBy;
        this.status = ReportStatus.GENERATING;
        this.generatedAt = LocalDateTime.now();
    }

    public static AnalyticsReport create(ReportType reportType, ReportPeriod reportPeriod,
                                          LocalDate reportDate, LocalDate startDate,
                                          LocalDate endDate, String generatedBy) {
        return new AnalyticsReport(ReportId.generate(), reportType, reportPeriod,
            reportDate, startDate, endDate, generatedBy);
    }

    public void complete(Long totalRecords, String dataSummary, String reportData) {
        if (this.status != ReportStatus.GENERATING) {
            throw new InvalidOperationException(
                "Cannot complete report in status: " + this.status);
        }
        this.status = ReportStatus.COMPLETED;
        this.totalRecords = totalRecords;
        this.dataSummary = dataSummary;
        this.reportData = reportData;
    }

    public void fail(String errorMessage) {
        if (this.status != ReportStatus.GENERATING) {
            throw new InvalidOperationException(
                "Cannot fail report in status: " + this.status);
        }
        this.status = ReportStatus.FAILED;
        this.failureReason = errorMessage;
    }

    public void cancel(String reason) {
        if (this.status == ReportStatus.COMPLETED) {
            throw new InvalidOperationException(
                "Cannot cancel a completed report");
        }
        this.status = ReportStatus.CANCELLED;
        this.failureReason = reason;
    }

    public void regenerate(String regeneratedBy) {
        if (this.status != ReportStatus.FAILED && this.status != ReportStatus.CANCELLED) {
            throw new InvalidOperationException(
                "Can only regenerate a failed or cancelled report, current status: " + this.status);
        }
        this.status = ReportStatus.GENERATING;
        this.generatedBy = regeneratedBy;
        this.generatedAt = LocalDateTime.now();
        this.totalRecords = null;
        this.dataSummary = null;
        this.reportData = null;
        this.failureReason = null;
    }

    public boolean isGenerating() {
        return this.status == ReportStatus.GENERATING;
    }

    public boolean isCompleted() {
        return this.status == ReportStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == ReportStatus.FAILED;
    }

    public boolean isCancelled() {
        return this.status == ReportStatus.CANCELLED;
    }

    // Getters
    public ReportId getId() { return id; }
    public ReportType getReportType() { return reportType; }
    public ReportPeriod getReportPeriod() { return reportPeriod; }
    public LocalDate getReportDate() { return reportDate; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public ReportStatus getStatus() { return status; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getGeneratedBy() { return generatedBy; }
    public Long getTotalRecords() { return totalRecords; }
    public String getDataSummary() { return dataSummary; }
    public String getReportData() { return reportData; }
    public String getFailureReason() { return failureReason; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
