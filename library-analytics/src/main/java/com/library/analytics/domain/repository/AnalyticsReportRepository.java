package com.library.analytics.domain.repository;

import com.library.analytics.domain.model.AnalyticsReport;
import com.library.analytics.domain.model.enums.ReportStatus;
import com.library.analytics.domain.model.enums.ReportType;
import com.library.shared.domain.model.ReportId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnalyticsReportRepository extends JpaRepository<AnalyticsReport, ReportId> {

    List<AnalyticsReport> findByReportType(ReportType reportType);

    List<AnalyticsReport> findByStatus(ReportStatus status);

    List<AnalyticsReport> findByReportTypeAndReportDateBetween(ReportType reportType,
                                                                LocalDate startDate,
                                                                LocalDate endDate);
}
