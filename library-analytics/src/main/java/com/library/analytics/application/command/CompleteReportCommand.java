package com.library.analytics.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CompleteReportCommand {

    @NotBlank(message = "Report ID must not be blank")
    private String reportId;

    @NotNull(message = "Total records must not be null")
    private Long totalRecords;

    private String dataSummary;

    private String reportData;

    public CompleteReportCommand() {
    }

    public CompleteReportCommand(String reportId, Long totalRecords, String dataSummary, String reportData) {
        this.reportId = reportId;
        this.totalRecords = totalRecords;
        this.dataSummary = dataSummary;
        this.reportData = reportData;
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public Long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Long totalRecords) { this.totalRecords = totalRecords; }
    public String getDataSummary() { return dataSummary; }
    public void setDataSummary(String dataSummary) { this.dataSummary = dataSummary; }
    public String getReportData() { return reportData; }
    public void setReportData(String reportData) { this.reportData = reportData; }
}
