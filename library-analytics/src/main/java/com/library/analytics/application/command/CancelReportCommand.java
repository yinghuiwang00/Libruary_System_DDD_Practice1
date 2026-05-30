package com.library.analytics.application.command;

import jakarta.validation.constraints.NotBlank;

public class CancelReportCommand {

    @NotBlank(message = "Report ID must not be blank")
    private String reportId;

    private String reason;

    public CancelReportCommand() {
    }

    public CancelReportCommand(String reportId, String reason) {
        this.reportId = reportId;
        this.reason = reason;
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
