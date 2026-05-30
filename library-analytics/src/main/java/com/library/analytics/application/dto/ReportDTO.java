package com.library.analytics.application.dto;

import com.library.analytics.domain.model.AnalyticsReport;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReportDTO {

    private String id;
    private String reportType;
    private String reportPeriod;
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private Long totalRecords;
    private String dataSummary;
    private String reportData;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReportDTO() {
    }

    public static ReportDTO from(AnalyticsReport report) {
        ReportDTO dto = new ReportDTO();
        dto.id = report.getId().getValue();
        dto.reportType = report.getReportType().name();
        dto.reportPeriod = report.getReportPeriod().name();
        dto.reportDate = report.getReportDate();
        dto.startDate = report.getStartDate();
        dto.endDate = report.getEndDate();
        dto.status = report.getStatus().name();
        dto.generatedAt = report.getGeneratedAt();
        dto.generatedBy = report.getGeneratedBy();
        dto.totalRecords = report.getTotalRecords();
        dto.dataSummary = report.getDataSummary();
        dto.reportData = report.getReportData();
        dto.version = report.getVersion();
        dto.createdAt = report.getCreatedAt();
        dto.updatedAt = report.getUpdatedAt();
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getReportPeriod() { return reportPeriod; }
    public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public Long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Long totalRecords) { this.totalRecords = totalRecords; }
    public String getDataSummary() { return dataSummary; }
    public void setDataSummary(String dataSummary) { this.dataSummary = dataSummary; }
    public String getReportData() { return reportData; }
    public void setReportData(String reportData) { this.reportData = reportData; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
