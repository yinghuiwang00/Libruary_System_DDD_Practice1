package com.library.analytics.domain.model;

import com.library.analytics.domain.exception.InvalidOperationException;
import com.library.analytics.domain.model.enums.ReportPeriod;
import com.library.analytics.domain.model.enums.ReportStatus;
import com.library.analytics.domain.model.enums.ReportType;
import com.library.shared.domain.model.ReportId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class AnalyticsReportTest {

    private AnalyticsReport createGeneratingReport() {
        return AnalyticsReport.create(
            ReportType.CIRCULATION_REPORT,
            ReportPeriod.MONTHLY,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31),
            "admin"
        );
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create report with GENERATING status via factory method")
        void shouldCreateReportWithGeneratingStatus() {
            AnalyticsReport report = createGeneratingReport();

            assertThat(report.getId()).isNotNull();
            assertThat(report.getReportType()).isEqualTo(ReportType.CIRCULATION_REPORT);
            assertThat(report.getReportPeriod()).isEqualTo(ReportPeriod.MONTHLY);
            assertThat(report.getReportDate()).isEqualTo(LocalDate.of(2026, 5, 1));
            assertThat(report.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
            assertThat(report.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
            assertThat(report.getGeneratedBy()).isEqualTo("admin");
            assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
        }

        @Test
        @DisplayName("should set generatedAt on creation")
        void shouldSetGeneratedAtOnCreation() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            AnalyticsReport report = createGeneratingReport();
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertThat(report.getGeneratedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should generate unique ID for each report")
        void shouldGenerateUniqueId() {
            AnalyticsReport report1 = createGeneratingReport();
            AnalyticsReport report2 = createGeneratingReport();

            assertThat(report1.getId()).isNotEqualTo(report2.getId());
        }

        @Test
        @DisplayName("should have null totalRecords, dataSummary, reportData initially")
        void shouldHaveNullDataFieldsInitially() {
            AnalyticsReport report = createGeneratingReport();

            assertThat(report.getTotalRecords()).isNull();
            assertThat(report.getDataSummary()).isNull();
            assertThat(report.getReportData()).isNull();
            assertThat(report.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("should throw when reportType is null")
        void shouldThrowWhenReportTypeIsNull() {
            assertThatThrownBy(() -> AnalyticsReport.create(
                null, ReportPeriod.MONTHLY, LocalDate.now(), null, null, "admin"
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Report type must not be null");
        }

        @Test
        @DisplayName("should throw when reportPeriod is null")
        void shouldThrowWhenReportPeriodIsNull() {
            assertThatThrownBy(() -> AnalyticsReport.create(
                ReportType.CIRCULATION_REPORT, null, LocalDate.now(), null, null, "admin"
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Report period must not be null");
        }

        @Test
        @DisplayName("should throw when reportDate is null")
        void shouldThrowWhenReportDateIsNull() {
            assertThatThrownBy(() -> AnalyticsReport.create(
                ReportType.CIRCULATION_REPORT, ReportPeriod.MONTHLY, null, null, null, "admin"
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Report date must not be null");
        }

        @Test
        @DisplayName("should allow null startDate and endDate")
        void shouldAllowNullStartAndEndDate() {
            AnalyticsReport report = AnalyticsReport.create(
                ReportType.CIRCULATION_REPORT,
                ReportPeriod.MONTHLY,
                LocalDate.of(2026, 5, 1),
                null,
                null,
                "admin"
            );

            assertThat(report.getStartDate()).isNull();
            assertThat(report.getEndDate()).isNull();
        }

        @Test
        @DisplayName("should allow null generatedBy")
        void shouldAllowNullGeneratedBy() {
            AnalyticsReport report = AnalyticsReport.create(
                ReportType.CIRCULATION_REPORT,
                ReportPeriod.MONTHLY,
                LocalDate.of(2026, 5, 1),
                null,
                null,
                null
            );

            assertThat(report.getGeneratedBy()).isNull();
        }

        @Test
        @DisplayName("isGenerating should return true for new report")
        void isGeneratingShouldReturnTrueForNewReport() {
            AnalyticsReport report = createGeneratingReport();

            assertThat(report.isGenerating()).isTrue();
            assertThat(report.isCompleted()).isFalse();
            assertThat(report.isFailed()).isFalse();
            assertThat(report.isCancelled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Complete")
    class Complete {

        @Test
        @DisplayName("should complete report from GENERATING status")
        void shouldCompleteReportFromGeneratingStatus() {
            AnalyticsReport report = createGeneratingReport();

            report.complete(100L, "Summary of data", "{data: [...]}");

            assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
            assertThat(report.getTotalRecords()).isEqualTo(100L);
            assertThat(report.getDataSummary()).isEqualTo("Summary of data");
            assertThat(report.getReportData()).isEqualTo("{data: [...]}");
        }

        @Test
        @DisplayName("should mark as completed")
        void shouldMarkAsCompleted() {
            AnalyticsReport report = createGeneratingReport();
            report.complete(50L, "summary", "data");

            assertThat(report.isCompleted()).isTrue();
            assertThat(report.isGenerating()).isFalse();
        }

        @Test
        @DisplayName("should throw when completing a COMPLETED report")
        void shouldThrowWhenCompletingCompletedReport() {
            AnalyticsReport report = createGeneratingReport();
            report.complete(100L, "summary", "data");

            assertThatThrownBy(() -> report.complete(200L, "new summary", "new data"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot complete report in status: COMPLETED");
        }

        @Test
        @DisplayName("should throw when completing a FAILED report")
        void shouldThrowWhenCompletingFailedReport() {
            AnalyticsReport report = createGeneratingReport();
            report.fail("error");

            assertThatThrownBy(() -> report.complete(100L, "summary", "data"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot complete report in status: FAILED");
        }

        @Test
        @DisplayName("should throw when completing a CANCELLED report")
        void shouldThrowWhenCompletingCancelledReport() {
            AnalyticsReport report = createGeneratingReport();
            report.cancel("reason");

            assertThatThrownBy(() -> report.complete(100L, "summary", "data"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot complete report in status: CANCELLED");
        }
    }

    @Nested
    @DisplayName("Fail")
    class Fail {

        @Test
        @DisplayName("should fail report from GENERATING status")
        void shouldFailReportFromGeneratingStatus() {
            AnalyticsReport report = createGeneratingReport();

            report.fail("Data source unavailable");

            assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
            assertThat(report.getFailureReason()).isEqualTo("Data source unavailable");
        }

        @Test
        @DisplayName("should mark as failed")
        void shouldMarkAsFailed() {
            AnalyticsReport report = createGeneratingReport();
            report.fail("timeout");

            assertThat(report.isFailed()).isTrue();
            assertThat(report.isGenerating()).isFalse();
        }

        @Test
        @DisplayName("should throw when failing a COMPLETED report")
        void shouldThrowWhenFailingCompletedReport() {
            AnalyticsReport report = createGeneratingReport();
            report.complete(100L, "summary", "data");

            assertThatThrownBy(() -> report.fail("error"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot fail report in status: COMPLETED");
        }

        @Test
        @DisplayName("should throw when failing a FAILED report")
        void shouldThrowWhenFailingFailedReport() {
            AnalyticsReport report = createGeneratingReport();
            report.fail("first error");

            assertThatThrownBy(() -> report.fail("second error"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot fail report in status: FAILED");
        }

        @Test
        @DisplayName("should throw when failing a CANCELLED report")
        void shouldThrowWhenFailingCancelledReport() {
            AnalyticsReport report = createGeneratingReport();
            report.cancel("reason");

            assertThatThrownBy(() -> report.fail("error"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot fail report in status: CANCELLED");
        }
    }

    @Nested
    @DisplayName("Cancel")
    class Cancel {

        @Test
        @DisplayName("should cancel report from GENERATING status")
        void shouldCancelReportFromGeneratingStatus() {
            AnalyticsReport report = createGeneratingReport();

            report.cancel("No longer needed");

            assertThat(report.getStatus()).isEqualTo(ReportStatus.CANCELLED);
            assertThat(report.getFailureReason()).isEqualTo("No longer needed");
        }

        @Test
        @DisplayName("should mark as cancelled")
        void shouldMarkAsCancelled() {
            AnalyticsReport report = createGeneratingReport();
            report.cancel("reason");

            assertThat(report.isCancelled()).isTrue();
            assertThat(report.isGenerating()).isFalse();
        }

        @Test
        @DisplayName("should cancel report from FAILED status")
        void shouldCancelReportFromFailedStatus() {
            AnalyticsReport report = createGeneratingReport();
            report.fail("error");

            report.cancel("cancelling after failure");

            assertThat(report.getStatus()).isEqualTo(ReportStatus.CANCELLED);
            assertThat(report.getFailureReason()).isEqualTo("cancelling after failure");
        }

        @Test
        @DisplayName("should throw when cancelling a COMPLETED report")
        void shouldThrowWhenCancellingCompletedReport() {
            AnalyticsReport report = createGeneratingReport();
            report.complete(100L, "summary", "data");

            assertThatThrownBy(() -> report.cancel("reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot cancel a completed report");
        }

        @Test
        @DisplayName("should cancel report from GENERATING status with null reason")
        void shouldCancelWithNullReason() {
            AnalyticsReport report = createGeneratingReport();

            report.cancel(null);

            assertThat(report.getStatus()).isEqualTo(ReportStatus.CANCELLED);
            assertThat(report.getFailureReason()).isNull();
        }
    }

    @Nested
    @DisplayName("Regenerate")
    class Regenerate {

        @Test
        @DisplayName("should regenerate report from FAILED status")
        void shouldRegenerateReportFromFailedStatus() {
            AnalyticsReport report = createGeneratingReport();
            report.fail("timeout");

            LocalDateTime beforeRegenerate = LocalDateTime.now().minusSeconds(1);
            report.regenerate("admin2");
            LocalDateTime afterRegenerate = LocalDateTime.now().plusSeconds(1);

            assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
            assertThat(report.getGeneratedBy()).isEqualTo("admin2");
            assertThat(report.getGeneratedAt()).isBetween(beforeRegenerate, afterRegenerate);
            assertThat(report.getTotalRecords()).isNull();
            assertThat(report.getDataSummary()).isNull();
            assertThat(report.getReportData()).isNull();
            assertThat(report.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("should regenerate report from CANCELLED status")
        void shouldRegenerateReportFromCancelledStatus() {
            AnalyticsReport report = createGeneratingReport();
            report.cancel("cancelled");

            report.regenerate("admin2");

            assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
            assertThat(report.getGeneratedBy()).isEqualTo("admin2");
            assertThat(report.getTotalRecords()).isNull();
            assertThat(report.getDataSummary()).isNull();
            assertThat(report.getReportData()).isNull();
            assertThat(report.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("should throw when regenerating from GENERATING status")
        void shouldThrowWhenRegeneratingFromGeneratingStatus() {
            AnalyticsReport report = createGeneratingReport();

            assertThatThrownBy(() -> report.regenerate("admin"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Can only regenerate a failed or cancelled report");
        }

        @Test
        @DisplayName("should throw when regenerating from COMPLETED status")
        void shouldThrowWhenRegeneratingFromCompletedStatus() {
            AnalyticsReport report = createGeneratingReport();
            report.complete(100L, "summary", "data");

            assertThatThrownBy(() -> report.regenerate("admin"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Can only regenerate a failed or cancelled report");
        }

        @Test
        @DisplayName("should clear previous data on regenerate")
        void shouldClearPreviousDataOnRegenerate() {
            AnalyticsReport report = createGeneratingReport();
            report.fail("error with data");

            report.regenerate("admin2");

            assertThat(report.getTotalRecords()).isNull();
            assertThat(report.getDataSummary()).isNull();
            assertThat(report.getReportData()).isNull();
            assertThat(report.getFailureReason()).isNull();
        }
    }

    @Nested
    @DisplayName("InvalidTransitions")
    class InvalidTransitions {

        @Test
        @DisplayName("should not allow complete on FAILED report")
        void shouldNotAllowCompleteOnFailed() {
            AnalyticsReport report = createGeneratingReport();
            report.fail("error");

            assertThatThrownBy(() -> report.complete(1L, "s", "d"))
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should not allow complete on CANCELLED report")
        void shouldNotAllowCompleteOnCancelled() {
            AnalyticsReport report = createGeneratingReport();
            report.cancel("reason");

            assertThatThrownBy(() -> report.complete(1L, "s", "d"))
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should not allow fail on COMPLETED report")
        void shouldNotAllowFailOnCompleted() {
            AnalyticsReport report = createGeneratingReport();
            report.complete(1L, "s", "d");

            assertThatThrownBy(() -> report.fail("error"))
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should not allow fail on CANCELLED report")
        void shouldNotAllowFailOnCancelled() {
            AnalyticsReport report = createGeneratingReport();
            report.cancel("reason");

            assertThatThrownBy(() -> report.fail("error"))
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should not allow cancel on COMPLETED report")
        void shouldNotAllowCancelOnCompleted() {
            AnalyticsReport report = createGeneratingReport();
            report.complete(1L, "s", "d");

            assertThatThrownBy(() -> report.cancel("reason"))
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should not allow regenerate on GENERATING report")
        void shouldNotAllowRegenerateOnGenerating() {
            AnalyticsReport report = createGeneratingReport();

            assertThatThrownBy(() -> report.regenerate("admin"))
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should not allow regenerate on COMPLETED report")
        void shouldNotAllowRegenerateOnCompleted() {
            AnalyticsReport report = createGeneratingReport();
            report.complete(1L, "s", "d");

            assertThatThrownBy(() -> report.regenerate("admin"))
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should not allow double complete")
        void shouldNotAllowDoubleComplete() {
            AnalyticsReport report = createGeneratingReport();
            report.complete(1L, "s", "d");

            assertThatThrownBy(() -> report.complete(2L, "s2", "d2"))
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should not allow double fail")
        void shouldNotAllowDoubleFail() {
            AnalyticsReport report = createGeneratingReport();
            report.fail("first");

            assertThatThrownBy(() -> report.fail("second"))
                .isInstanceOf(InvalidOperationException.class);
        }
    }

    @Nested
    @DisplayName("StatusChecks")
    class StatusChecks {

        @Test
        @DisplayName("isGenerating should reflect current status")
        void isGeneratingShouldReflectCurrentStatus() {
            AnalyticsReport report = createGeneratingReport();
            assertThat(report.isGenerating()).isTrue();

            report.complete(1L, "s", "d");
            assertThat(report.isGenerating()).isFalse();
        }

        @Test
        @DisplayName("isCompleted should reflect current status")
        void isCompletedShouldReflectCurrentStatus() {
            AnalyticsReport report = createGeneratingReport();
            assertThat(report.isCompleted()).isFalse();

            report.complete(1L, "s", "d");
            assertThat(report.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("isFailed should reflect current status")
        void isFailedShouldReflectCurrentStatus() {
            AnalyticsReport report = createGeneratingReport();
            assertThat(report.isFailed()).isFalse();

            report.fail("error");
            assertThat(report.isFailed()).isTrue();
        }

        @Test
        @DisplayName("isCancelled should reflect current status")
        void isCancelledShouldReflectCurrentStatus() {
            AnalyticsReport report = createGeneratingReport();
            assertThat(report.isCancelled()).isFalse();

            report.cancel("reason");
            assertThat(report.isCancelled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Getters")
    class Getters {

        @Test
        @DisplayName("should return all field values correctly")
        void shouldReturnAllFieldValuesCorrectly() {
            LocalDate reportDate = LocalDate.of(2026, 5, 1);
            LocalDate startDate = LocalDate.of(2026, 5, 1);
            LocalDate endDate = LocalDate.of(2026, 5, 31);

            AnalyticsReport report = AnalyticsReport.create(
                ReportType.INVENTORY_REPORT,
                ReportPeriod.QUARTERLY,
                reportDate,
                startDate,
                endDate,
                "system"
            );

            assertThat(report.getId()).isNotNull();
            assertThat(report.getId()).isInstanceOf(ReportId.class);
            assertThat(report.getReportType()).isEqualTo(ReportType.INVENTORY_REPORT);
            assertThat(report.getReportPeriod()).isEqualTo(ReportPeriod.QUARTERLY);
            assertThat(report.getReportDate()).isEqualTo(reportDate);
            assertThat(report.getStartDate()).isEqualTo(startDate);
            assertThat(report.getEndDate()).isEqualTo(endDate);
            assertThat(report.getGeneratedBy()).isEqualTo("system");
            assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
        }

        @Test
        @DisplayName("should support all report types")
        void shouldSupportAllReportTypes() {
            for (ReportType type : ReportType.values()) {
                AnalyticsReport report = AnalyticsReport.create(
                    type, ReportPeriod.MONTHLY, LocalDate.now(), null, null, "admin"
                );
                assertThat(report.getReportType()).isEqualTo(type);
            }
        }

        @Test
        @DisplayName("should support all report periods")
        void shouldSupportAllReportPeriods() {
            for (ReportPeriod period : ReportPeriod.values()) {
                AnalyticsReport report = AnalyticsReport.create(
                    ReportType.CIRCULATION_REPORT, period, LocalDate.now(), null, null, "admin"
                );
                assertThat(report.getReportPeriod()).isEqualTo(period);
            }
        }
    }
}
