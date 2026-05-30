package com.library.analytics.domain.event;

import com.library.analytics.domain.model.enums.ReportType;
import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.ReportId;

public class ReportCreatedEvent extends DomainEvent {

    private final ReportId reportId;
    private final ReportType reportType;
    private final String generatedBy;

    public ReportCreatedEvent(ReportId reportId, ReportType reportType, String generatedBy) {
        super();
        this.reportId = reportId;
        this.reportType = reportType;
        this.generatedBy = generatedBy;
    }

    public ReportId getReportId() { return reportId; }
    public ReportType getReportType() { return reportType; }
    public String getGeneratedBy() { return generatedBy; }
}
