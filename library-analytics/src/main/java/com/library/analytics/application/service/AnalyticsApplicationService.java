package com.library.analytics.application.service;

import com.library.analytics.application.command.*;
import com.library.analytics.application.dto.ReportDTO;
import com.library.analytics.domain.model.AnalyticsReport;
import com.library.analytics.domain.model.enums.ReportPeriod;
import com.library.analytics.domain.model.enums.ReportType;
import com.library.analytics.domain.model.enums.ReportStatus;
import com.library.analytics.domain.service.AnalyticsService;
import com.library.shared.domain.model.ReportId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnalyticsApplicationService {

    private final AnalyticsService analyticsService;

    public AnalyticsApplicationService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Transactional
    public ReportDTO createReport(CreateReportCommand command) {
        AnalyticsReport report = analyticsService.createReport(
                ReportType.valueOf(command.getReportType()),
                ReportPeriod.valueOf(command.getReportPeriod()),
                command.getReportDate(),
                command.getStartDate(),
                command.getEndDate(),
                command.getGeneratedBy()
        );
        return ReportDTO.from(report);
    }

    @Transactional
    public ReportDTO completeReport(CompleteReportCommand command) {
        AnalyticsReport report = analyticsService.completeReport(
                ReportId.of(command.getReportId()),
                command.getTotalRecords(),
                command.getDataSummary(),
                command.getReportData()
        );
        return ReportDTO.from(report);
    }

    @Transactional
    public ReportDTO failReport(FailReportCommand command) {
        AnalyticsReport report = analyticsService.failReport(
                ReportId.of(command.getReportId()),
                command.getErrorMessage()
        );
        return ReportDTO.from(report);
    }

    @Transactional
    public ReportDTO cancelReport(CancelReportCommand command) {
        AnalyticsReport report = analyticsService.cancelReport(
                ReportId.of(command.getReportId()),
                command.getReason()
        );
        return ReportDTO.from(report);
    }

    @Transactional
    public ReportDTO regenerateReport(RegenerateReportCommand command) {
        AnalyticsReport report = analyticsService.regenerateReport(
                ReportId.of(command.getReportId()),
                command.getRegeneratedBy()
        );
        return ReportDTO.from(report);
    }

    @Transactional(readOnly = true)
    public ReportDTO getReport(String reportId) {
        AnalyticsReport report = analyticsService.getReport(ReportId.of(reportId));
        return ReportDTO.from(report);
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> getAllReports() {
        return analyticsService.getAllReports().stream()
                .map(ReportDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> getReportsByType(String reportType) {
        return analyticsService.getReportsByType(ReportType.valueOf(reportType)).stream()
                .map(ReportDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> getReportsByStatus(String status) {
        return analyticsService.getReportsByStatus(ReportStatus.valueOf(status)).stream()
                .map(ReportDTO::from)
                .collect(Collectors.toList());
    }
}
