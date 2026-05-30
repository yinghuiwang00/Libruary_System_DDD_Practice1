package com.library.analytics.application.command;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateReportCommand {

    @NotNull(message = "Report type must not be null")
    private String reportType;

    @NotNull(message = "Report period must not be null")
    private String reportPeriod;

    @NotNull(message = "Report date must not be null")
    private LocalDate reportDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private String generatedBy;

    public CreateReportCommand() {
    }

    public CreateReportCommand(String reportType, String reportPeriod, LocalDate reportDate,
                               LocalDate startDate, LocalDate endDate, String generatedBy) {
        this.reportType = reportType;
        this.reportPeriod = reportPeriod;
        this.reportDate = reportDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.generatedBy = generatedBy;
    }

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
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
}
