package com.library.analytics.domain.event;

import com.library.analytics.domain.model.enums.ReportType;
import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.ReportId;

public class ReportCompletedEvent extends DomainEvent {

    private final ReportId reportId;
    private final ReportType reportType;
    private final Long totalRecords;

    public ReportCompletedEvent(ReportId reportId, ReportType reportType, Long totalRecords) {
        super();
        this.reportId = reportId;
        this.reportType = reportType;
        this.totalRecords = totalRecords;
    }

    public ReportId getReportId() { return reportId; }
    public ReportType getReportType() { return reportType; }
    public Long getTotalRecords() { return totalRecords; }
}
