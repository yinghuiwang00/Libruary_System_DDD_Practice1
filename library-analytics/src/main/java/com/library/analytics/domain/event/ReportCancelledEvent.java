package com.library.analytics.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.ReportId;

public class ReportCancelledEvent extends DomainEvent {

    private final ReportId reportId;
    private final String reason;

    public ReportCancelledEvent(ReportId reportId, String reason) {
        super();
        this.reportId = reportId;
        this.reason = reason;
    }

    public ReportId getReportId() { return reportId; }
    public String getReason() { return reason; }
}
