package com.library.analytics.domain.exception;

import com.library.shared.domain.model.ReportId;

public class ReportNotFoundException extends DomainException {

    public ReportNotFoundException(ReportId reportId) {
        super("REPORT_NOT_FOUND", "Report not found: " + reportId);
    }
}
