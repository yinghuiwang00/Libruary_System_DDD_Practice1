package com.library.analytics.application.command;

import jakarta.validation.constraints.NotBlank;

public class FailReportCommand {

    @NotBlank(message = "Report ID must not be blank")
    private String reportId;

    private String errorMessage;

    public FailReportCommand() {
    }

    public FailReportCommand(String reportId, String errorMessage) {
        this.reportId = reportId;
        this.errorMessage = errorMessage;
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
