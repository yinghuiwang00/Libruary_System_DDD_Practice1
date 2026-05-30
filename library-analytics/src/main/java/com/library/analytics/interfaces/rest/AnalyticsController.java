package com.library.analytics.interfaces.rest;

import com.library.analytics.application.command.*;
import com.library.analytics.application.dto.ApiResponse;
import com.library.analytics.application.dto.ReportDTO;
import com.library.analytics.application.service.AnalyticsApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics Management", description = "APIs for managing analytics reports")
public class AnalyticsController {

    private final AnalyticsApplicationService analyticsService;

    public AnalyticsController(AnalyticsApplicationService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/reports")
    @Operation(summary = "Create a new analytics report")
    public ResponseEntity<ApiResponse<ReportDTO>> createReport(@Valid @RequestBody CreateReportCommand command) {
        ReportDTO report = analyticsService.createReport(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(report));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get reports with optional filters")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReports(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        if (type != null) {
            List<ReportDTO> reports = analyticsService.getReportsByType(type);
            return ResponseEntity.ok(ApiResponse.success(reports));
        }
        if (status != null) {
            List<ReportDTO> reports = analyticsService.getReportsByStatus(status);
            return ResponseEntity.ok(ApiResponse.success(reports));
        }
        List<ReportDTO> reports = analyticsService.getAllReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get a report by ID")
    public ResponseEntity<ApiResponse<ReportDTO>> getReport(@PathVariable String id) {
        ReportDTO report = analyticsService.getReport(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/reports/{id}/complete")
    @Operation(summary = "Complete a report with data")
    public ResponseEntity<ApiResponse<ReportDTO>> completeReport(
            @PathVariable String id,
            @Valid @RequestBody CompleteReportCommand request) {
        CompleteReportCommand command = new CompleteReportCommand(
                id,
                request.getTotalRecords(),
                request.getDataSummary(),
                request.getReportData()
        );
        ReportDTO report = analyticsService.completeReport(command);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/reports/{id}/fail")
    @Operation(summary = "Mark a report as failed")
    public ResponseEntity<ApiResponse<ReportDTO>> failReport(
            @PathVariable String id,
            @RequestBody FailReportCommand request) {
        FailReportCommand command = new FailReportCommand(id, request.getErrorMessage());
        ReportDTO report = analyticsService.failReport(command);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/reports/{id}/cancel")
    @Operation(summary = "Cancel a report")
    public ResponseEntity<ApiResponse<ReportDTO>> cancelReport(
            @PathVariable String id,
            @RequestBody CancelReportCommand request) {
        CancelReportCommand command = new CancelReportCommand(id, request.getReason());
        ReportDTO report = analyticsService.cancelReport(command);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/reports/{id}/regenerate")
    @Operation(summary = "Regenerate a report")
    public ResponseEntity<ApiResponse<ReportDTO>> regenerateReport(
            @PathVariable String id,
            @RequestBody RegenerateReportCommand request) {
        RegenerateReportCommand command = new RegenerateReportCommand(id, request.getRegeneratedBy());
        ReportDTO report = analyticsService.regenerateReport(command);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
