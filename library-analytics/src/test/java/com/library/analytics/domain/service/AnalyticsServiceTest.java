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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsReportRepository reportRepository;

    @Mock
    private AnalyticsDomainEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<ReportCreatedEvent> createdEventCaptor;

    @Captor
    private ArgumentCaptor<ReportCompletedEvent> completedEventCaptor;

    @Captor
    private ArgumentCaptor<ReportFailedEvent> failedEventCaptor;

    @Captor
    private ArgumentCaptor<ReportCancelledEvent> cancelledEventCaptor;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(reportRepository, eventPublisher);
    }

    private AnalyticsReport createReport() {
        return AnalyticsReport.create(
            ReportType.CIRCULATION_REPORT,
            ReportPeriod.MONTHLY,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31),
            "admin"
        );
    }

    private AnalyticsReport stubReportFound(AnalyticsReport report) {
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        return report;
    }

    private AnalyticsReport stubReportSave(AnalyticsReport report) {
        when(reportRepository.save(any(AnalyticsReport.class))).thenReturn(report);
        return report;
    }

    private AnalyticsReport stubReportSaveReturnsArg() {
        when(reportRepository.save(any(AnalyticsReport.class))).thenAnswer(inv -> inv.getArgument(0));
        return null;
    }

    @Nested
    @DisplayName("createReport")
    class CreateReport {

        @Test
        @DisplayName("should create report and persist it")
        void shouldCreateReportAndPersist() {
            stubReportSaveReturnsArg();

            AnalyticsReport report = analyticsService.createReport(
                ReportType.CIRCULATION_REPORT,
                ReportPeriod.MONTHLY,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "admin"
            );

            assertThat(report).isNotNull();
            assertThat(report.getReportType()).isEqualTo(ReportType.CIRCULATION_REPORT);
            assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
            verify(reportRepository).save(any(AnalyticsReport.class));
        }

        @Test
        @DisplayName("should publish ReportCreatedEvent on creation")
        void shouldPublishCreatedEvent() {
            stubReportSaveReturnsArg();

            analyticsService.createReport(
                ReportType.CIRCULATION_REPORT,
                ReportPeriod.MONTHLY,
                LocalDate.of(2026, 5, 1),
                null, null, "admin"
            );

            verify(eventPublisher).publish(createdEventCaptor.capture());
            ReportCreatedEvent event = createdEventCaptor.getValue();
            assertThat(event.getReportType()).isEqualTo(ReportType.CIRCULATION_REPORT);
            assertThat(event.getGeneratedBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should return saved report from repository")
        void shouldReturnSavedReport() {
            AnalyticsReport report = createReport();
            stubReportSave(report);

            AnalyticsReport result = analyticsService.createReport(
                ReportType.CIRCULATION_REPORT,
                ReportPeriod.MONTHLY,
                LocalDate.of(2026, 5, 1),
                null, null, "admin"
            );

            assertThat(result.getId()).isEqualTo(report.getId());
        }
    }

    @Nested
    @DisplayName("completeReport")
    class CompleteReport {

        @Test
        @DisplayName("should complete a GENERATING report")
        void shouldCompleteGeneratingReport() {
            AnalyticsReport report = createReport();
            stubReportFound(report);
            stubReportSaveReturnsArg();

            AnalyticsReport result = analyticsService.completeReport(
                report.getId(), 100L, "summary", "{data: [...]}");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.COMPLETED);
            assertThat(result.getTotalRecords()).isEqualTo(100L);
            verify(reportRepository).save(any(AnalyticsReport.class));
        }

        @Test
        @DisplayName("should publish ReportCompletedEvent on completion")
        void shouldPublishCompletedEvent() {
            AnalyticsReport report = createReport();
            stubReportFound(report);
            stubReportSaveReturnsArg();

            analyticsService.completeReport(report.getId(), 100L, "summary", "data");

            verify(eventPublisher).publish(completedEventCaptor.capture());
            ReportCompletedEvent event = completedEventCaptor.getValue();
            assertThat(event.getTotalRecords()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should throw when report not found")
        void shouldThrowWhenReportNotFound() {
            ReportId unknownId = ReportId.generate();
            when(reportRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analyticsService.completeReport(
                unknownId, 100L, "summary", "data"))
                .isInstanceOf(ReportNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when report is not in GENERATING status")
        void shouldThrowWhenNotGenerating() {
            AnalyticsReport report = createReport();
            report.complete(50L, "s", "d");
            stubReportFound(report);

            assertThatThrownBy(() -> analyticsService.completeReport(
                report.getId(), 100L, "summary", "data"))
                .isInstanceOf(InvalidOperationException.class);
        }
    }

    @Nested
    @DisplayName("failReport")
    class FailReport {

        @Test
        @DisplayName("should fail a GENERATING report")
        void shouldFailGeneratingReport() {
            AnalyticsReport report = createReport();
            stubReportFound(report);
            stubReportSaveReturnsArg();

            AnalyticsReport result = analyticsService.failReport(
                report.getId(), "Data unavailable");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.FAILED);
            assertThat(result.getFailureReason()).isEqualTo("Data unavailable");
        }

        @Test
        @DisplayName("should publish ReportFailedEvent on failure")
        void shouldPublishFailedEvent() {
            AnalyticsReport report = createReport();
            stubReportFound(report);
            stubReportSaveReturnsArg();

            analyticsService.failReport(report.getId(), "timeout");

            verify(eventPublisher).publish(failedEventCaptor.capture());
            ReportFailedEvent event = failedEventCaptor.getValue();
            assertThat(event.getErrorMessage()).isEqualTo("timeout");
        }

        @Test
        @DisplayName("should throw when report not found")
        void shouldThrowWhenReportNotFound() {
            ReportId unknownId = ReportId.generate();
            when(reportRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analyticsService.failReport(unknownId, "error"))
                .isInstanceOf(ReportNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancelReport")
    class CancelReport {

        @Test
        @DisplayName("should cancel a GENERATING report")
        void shouldCancelGeneratingReport() {
            AnalyticsReport report = createReport();
            stubReportFound(report);
            stubReportSaveReturnsArg();

            AnalyticsReport result = analyticsService.cancelReport(
                report.getId(), "Not needed");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.CANCELLED);
        }

        @Test
        @DisplayName("should publish ReportCancelledEvent on cancellation")
        void shouldPublishCancelledEvent() {
            AnalyticsReport report = createReport();
            stubReportFound(report);
            stubReportSaveReturnsArg();

            analyticsService.cancelReport(report.getId(), "reason");

            verify(eventPublisher).publish(cancelledEventCaptor.capture());
            ReportCancelledEvent event = cancelledEventCaptor.getValue();
            assertThat(event.getReason()).isEqualTo("reason");
        }

        @Test
        @DisplayName("should throw when report not found")
        void shouldThrowWhenReportNotFound() {
            ReportId unknownId = ReportId.generate();
            when(reportRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analyticsService.cancelReport(unknownId, "reason"))
                .isInstanceOf(ReportNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("regenerateReport")
    class RegenerateReport {

        @Test
        @DisplayName("should regenerate a FAILED report")
        void shouldRegenerateFailedReport() {
            AnalyticsReport report = createReport();
            report.fail("error");
            stubReportFound(report);
            stubReportSaveReturnsArg();

            AnalyticsReport result = analyticsService.regenerateReport(
                report.getId(), "admin2");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.GENERATING);
            assertThat(result.getGeneratedBy()).isEqualTo("admin2");
        }

        @Test
        @DisplayName("should regenerate a CANCELLED report")
        void shouldRegenerateCancelledReport() {
            AnalyticsReport report = createReport();
            report.cancel("cancelled");
            stubReportFound(report);
            stubReportSaveReturnsArg();

            AnalyticsReport result = analyticsService.regenerateReport(
                report.getId(), "admin2");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.GENERATING);
        }

        @Test
        @DisplayName("should publish ReportCreatedEvent on regeneration")
        void shouldPublishCreatedEventOnRegeneration() {
            AnalyticsReport report = createReport();
            report.fail("error");
            stubReportFound(report);
            stubReportSaveReturnsArg();

            analyticsService.regenerateReport(report.getId(), "admin2");

            verify(eventPublisher).publish(createdEventCaptor.capture());
            assertThat(createdEventCaptor.getValue().getGeneratedBy()).isEqualTo("admin2");
        }

        @Test
        @DisplayName("should throw when report not found")
        void shouldThrowWhenReportNotFound() {
            ReportId unknownId = ReportId.generate();
            when(reportRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analyticsService.regenerateReport(unknownId, "admin"))
                .isInstanceOf(ReportNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when trying to regenerate a COMPLETED report")
        void shouldThrowWhenRegeneratingCompletedReport() {
            AnalyticsReport report = createReport();
            report.complete(100L, "summary", "data");
            stubReportFound(report);

            assertThatThrownBy(() -> analyticsService.regenerateReport(report.getId(), "admin"))
                .isInstanceOf(InvalidOperationException.class);
        }
    }

    @Nested
    @DisplayName("getReport")
    class GetReport {

        @Test
        @DisplayName("should return report when found")
        void shouldReturnReportWhenFound() {
            AnalyticsReport report = createReport();
            stubReportFound(report);

            AnalyticsReport result = analyticsService.getReport(report.getId());

            assertThat(result).isEqualTo(report);
            verify(reportRepository).findById(report.getId());
        }

        @Test
        @DisplayName("should throw ReportNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            ReportId unknownId = ReportId.generate();
            when(reportRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analyticsService.getReport(unknownId))
                .isInstanceOf(ReportNotFoundException.class)
                .hasMessageContaining(unknownId.getValue());
        }
    }

    @Nested
    @DisplayName("getAllReports")
    class GetAllReports {

        @Test
        @DisplayName("should return all reports")
        void shouldReturnAllReports() {
            AnalyticsReport report1 = createReport();
            AnalyticsReport report2 = AnalyticsReport.create(
                ReportType.INVENTORY_REPORT, ReportPeriod.WEEKLY,
                LocalDate.now(), null, null, "system"
            );
            when(reportRepository.findAll()).thenReturn(List.of(report1, report2));

            List<AnalyticsReport> results = analyticsService.getAllReports();

            assertThat(results).hasSize(2)
                .containsExactly(report1, report2);
            verify(reportRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no reports")
        void shouldReturnEmptyList() {
            when(reportRepository.findAll()).thenReturn(List.of());

            List<AnalyticsReport> results = analyticsService.getAllReports();

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("getReportsByType")
    class GetReportsByType {

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            AnalyticsReport report = createReport();
            when(reportRepository.findByReportType(ReportType.CIRCULATION_REPORT))
                .thenReturn(List.of(report));

            List<AnalyticsReport> results = analyticsService.getReportsByType(
                ReportType.CIRCULATION_REPORT);

            assertThat(results).hasSize(1)
                .containsExactly(report);
            verify(reportRepository).findByReportType(ReportType.CIRCULATION_REPORT);
        }

        @Test
        @DisplayName("should return empty list for type with no reports")
        void shouldReturnEmptyListForTypeWithNoReports() {
            when(reportRepository.findByReportType(ReportType.FINANCIAL_REPORT))
                .thenReturn(List.of());

            List<AnalyticsReport> results = analyticsService.getReportsByType(
                ReportType.FINANCIAL_REPORT);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("getReportsByStatus")
    class GetReportsByStatus {

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            AnalyticsReport report = createReport();
            when(reportRepository.findByStatus(ReportStatus.GENERATING))
                .thenReturn(List.of(report));

            List<AnalyticsReport> results = analyticsService.getReportsByStatus(
                ReportStatus.GENERATING);

            assertThat(results).hasSize(1)
                .containsExactly(report);
            verify(reportRepository).findByStatus(ReportStatus.GENERATING);
        }

        @Test
        @DisplayName("should return empty list for status with no reports")
        void shouldReturnEmptyListForStatusWithNoReports() {
            when(reportRepository.findByStatus(ReportStatus.FAILED))
                .thenReturn(List.of());

            List<AnalyticsReport> results = analyticsService.getReportsByStatus(
                ReportStatus.FAILED);

            assertThat(results).isEmpty();
        }
    }
}
