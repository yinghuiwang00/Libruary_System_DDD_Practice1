package com.library.analytics.domain.event;

import com.library.analytics.domain.model.enums.ReportType;
import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.ReportId;

public class ReportFailedEvent extends DomainEvent {

    private final ReportId reportId;
    private final ReportType reportType;
    private final String errorMessage;

    public ReportFailedEvent(ReportId reportId, ReportType reportType, String errorMessage) {
        super();
        this.reportId = reportId;
        this.reportType = reportType;
        this.errorMessage = errorMessage;
    }

    public ReportId getReportId() { return reportId; }
    public ReportType getReportType() { return reportType; }
    public String getErrorMessage() { return errorMessage; }
}
