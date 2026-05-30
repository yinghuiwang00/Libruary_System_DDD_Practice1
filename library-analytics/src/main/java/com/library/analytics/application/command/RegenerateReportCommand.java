package com.library.analytics.application.command;

import jakarta.validation.constraints.NotBlank;

public class RegenerateReportCommand {

    @NotBlank(message = "Report ID must not be blank")
    private String reportId;

    private String regeneratedBy;

    public RegenerateReportCommand() {
    }

    public RegenerateReportCommand(String reportId, String regeneratedBy) {
        this.reportId = reportId;
        this.regeneratedBy = regeneratedBy;
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getRegeneratedBy() { return regeneratedBy; }
    public void setRegeneratedBy(String regeneratedBy) { this.regeneratedBy = regeneratedBy; }
}
