package com.library.analytics.domain.exception;

public class DashboardNotFoundException extends DomainException {

    public DashboardNotFoundException(String dashboardType) {
        super("DASHBOARD_NOT_FOUND", "Dashboard not found: " + dashboardType);
    }
}
