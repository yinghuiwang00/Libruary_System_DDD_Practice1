package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class ReportId extends AggregateId {

    protected ReportId() {
    }

    private ReportId(String value) {
        super(value);
    }

    public static ReportId generate() {
        return new ReportId(generateUUID());
    }

    public static ReportId of(String value) {
        return new ReportId(value);
    }
}
