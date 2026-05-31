package com.library.analytics.domain.service;

import com.library.analytics.domain.event.*;
import com.library.analytics.domain.exception.InvalidOperationException;
import com.library.analytics.domain.exception.ReportNotFoundException;
import com.library.analytics.domain.model.AnalyticsReport;
import com.library.analytics.domain.model.enums.ReportPeriod;
import com.library.analytics.domain.model.enums.ReportStatus;
import com.library.analytics.domain.model.enums.ReportType;
import com.library.analytics.domain.repository.AnalyticsReportRepository;
import com.library.analytics.infrastructure.messaging.AnalyticsDomainEventPublisher;
import com.library.shared.domain.model.ReportId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsReportRepository reportRepository;
    private final AnalyticsDomainEventPublisher eventPublisher;

    public AnalyticsService(AnalyticsReportRepository reportRepository,
                            AnalyticsDomainEventPublisher eventPublisher) {
        this.reportRepository = reportRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AnalyticsReport createReport(ReportType reportType, ReportPeriod reportPeriod,
                                         LocalDate reportDate, LocalDate startDate,
                                         LocalDate endDate, String generatedBy) {
        AnalyticsReport report = AnalyticsReport.create(
            reportType, reportPeriod, reportDate, startDate, endDate, generatedBy);
        AnalyticsReport saved = reportRepository.save(report);

        eventPublisher.publish(new ReportCreatedEvent(
            saved.getId(), saved.getReportType(), saved.getGeneratedBy()));
        return saved;
    }

    @Transactional
    public AnalyticsReport completeReport(ReportId reportId, Long totalRecords,
                                           String dataSummary, String reportData) {
        AnalyticsReport report = findOrThrow(reportId);
        report.complete(totalRecords, dataSummary, reportData);
        AnalyticsReport saved = reportRepository.save(report);

        eventPublisher.publish(new ReportCompletedEvent(
            saved.getId(), saved.getReportType(), saved.getTotalRecords()));
        return saved;
    }

    @Transactional
    public AnalyticsReport failReport(ReportId reportId, String errorMessage) {
        AnalyticsReport report = findOrThrow(reportId);
        report.fail(errorMessage);
        AnalyticsReport saved = reportRepository.save(report);

        eventPublisher.publish(new ReportFailedEvent(
            saved.getId(), saved.getReportType(), errorMessage));
        return saved;
    }

    @Transactional
    public AnalyticsReport cancelReport(ReportId reportId, String reason) {
        AnalyticsReport report = findOrThrow(reportId);
        report.cancel(reason);
        AnalyticsReport saved = reportRepository.save(report);

        eventPublisher.publish(new ReportCancelledEvent(saved.getId(), reason));
        return saved;
    }

    @Transactional
    public AnalyticsReport regenerateReport(ReportId reportId, String regeneratedBy) {
        AnalyticsReport report = findOrThrow(reportId);
        report.regenerate(regeneratedBy);
        AnalyticsReport saved = reportRepository.save(report);

        eventPublisher.publish(new ReportCreatedEvent(
            saved.getId(), saved.getReportType(), saved.getGeneratedBy()));
        return saved;
    }

    public AnalyticsReport getReport(ReportId reportId) {
        return findOrThrow(reportId);
    }

    public List<AnalyticsReport> getAllReports() {
        return reportRepository.findAll();
    }

    public List<AnalyticsReport> getReportsByType(ReportType reportType) {
        return reportRepository.findByReportType(reportType);
    }

    public List<AnalyticsReport> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatus(status);
    }

    private AnalyticsReport findOrThrow(ReportId reportId) {
        return reportRepository.findById(reportId)
            .orElseThrow(() -> new ReportNotFoundException(reportId));
    }
}
