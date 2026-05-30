package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class DashboardId extends AggregateId {

    protected DashboardId() {
    }

    private DashboardId(String value) {
        super(value);
    }

    public static DashboardId generate() {
        return new DashboardId(generateUUID());
    }

    public static DashboardId of(String value) {
        return new DashboardId(value);
    }
}
