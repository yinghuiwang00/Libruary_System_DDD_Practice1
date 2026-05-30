package com.library.analytics.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.analytics.application.command.*;
import com.library.analytics.application.dto.ApiResponse;
import com.library.analytics.application.dto.ReportDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateReportCommand createReportCommand() {
        return new CreateReportCommand(
            "CIRCULATION_REPORT",
            "MONTHLY",
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31),
            "admin"
        );
    }

    private String createReportAndGetId() throws Exception {
        CreateReportCommand command = createReportCommand();
        MvcResult result = mockMvc.perform(post("/api/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isCreated())
            .andReturn();

        ApiResponse<ReportDTO> response = readReportResponse(result);
        return response.getData().getId();
    }

    @Nested
    @DisplayName("POST /api/analytics/reports")
    class CreateReport {

        @Test
        @DisplayName("should create report and return 201")
        void shouldCreateReport() throws Exception {
            CreateReportCommand command = createReportCommand();

            mockMvc.perform(post("/api/analytics/reports")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportType").value("CIRCULATION_REPORT"))
                .andExpect(jsonPath("$.data.reportPeriod").value("MONTHLY"))
                .andExpect(jsonPath("$.data.status").value("GENERATING"))
                .andExpect(jsonPath("$.data.generatedBy").value("admin"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 for invalid report type")
        void shouldReturn400ForInvalidReportType() throws Exception {
            CreateReportCommand command = new CreateReportCommand(
                "INVALID_TYPE", "MONTHLY", LocalDate.now(), null, null, "admin"
            );

            mockMvc.perform(post("/api/analytics/reports")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/reports")
    class GetReports {

        @Test
        @DisplayName("should return all reports")
        void shouldReturnAllReports() throws Exception {
            createReportAndGetId();
            createReportAndGetId();

            mockMvc.perform(get("/api/analytics/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("should return empty list when no reports")
        void shouldReturnEmptyListWhenNoReports() throws Exception {
            mockMvc.perform(get("/api/analytics/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("should filter reports by type")
        void shouldFilterByType() throws Exception {
            createReportAndGetId();

            CreateReportCommand inventoryCommand = new CreateReportCommand(
                "INVENTORY_REPORT", "WEEKLY", LocalDate.now(), null, null, "system"
            );
            mockMvc.perform(post("/api/analytics/reports")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inventoryCommand)))
                .andExpect(status().isCreated());

            mockMvc.perform(get("/api/analytics/reports")
                    .param("type", "CIRCULATION_REPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].reportType").value("CIRCULATION_REPORT"));
        }

        @Test
        @DisplayName("should filter reports by status")
        void shouldFilterByStatus() throws Exception {
            String reportId = createReportAndGetId();

            CompleteReportCommand completeCmd = new CompleteReportCommand(
                reportId, 50L, "summary", "data"
            );
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeCmd)))
                .andExpect(status().isOk());

            mockMvc.perform(get("/api/analytics/reports")
                    .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/reports/{id}")
    class GetReportById {

        @Test
        @DisplayName("should return report by id")
        void shouldReturnReportById() throws Exception {
            String reportId = createReportAndGetId();

            mockMvc.perform(get("/api/analytics/reports/" + reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(reportId))
                .andExpect(jsonPath("$.data.status").value("GENERATING"));
        }

        @Test
        @DisplayName("should return 404 for non-existent report")
        void shouldReturn404ForNonExistentReport() throws Exception {
            mockMvc.perform(get("/api/analytics/reports/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/analytics/reports/{id}/complete")
    class CompleteReport {

        @Test
        @DisplayName("should complete a generating report")
        void shouldCompleteGeneratingReport() throws Exception {
            String reportId = createReportAndGetId();

            CompleteReportCommand command = new CompleteReportCommand(
                reportId, 100L, "Monthly circulation summary", "{records: [...]}"
            );

            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.totalRecords").value(100))
                .andExpect(jsonPath("$.data.dataSummary").value("Monthly circulation summary"));
        }

        @Test
        @DisplayName("should return 409 when completing a completed report")
        void shouldReturn409WhenCompletingCompletedReport() throws Exception {
            String reportId = createReportAndGetId();

            CompleteReportCommand command = new CompleteReportCommand(
                reportId, 100L, "summary", "data"
            );
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/analytics/reports/{id}/fail")
    class FailReport {

        @Test
        @DisplayName("should fail a generating report")
        void shouldFailGeneratingReport() throws Exception {
            String reportId = createReportAndGetId();

            FailReportCommand command = new FailReportCommand(reportId, "Data source timeout");

            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/fail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAILED"));
        }

        @Test
        @DisplayName("should return 409 when failing a completed report")
        void shouldReturn409WhenFailingCompletedReport() throws Exception {
            String reportId = createReportAndGetId();

            CompleteReportCommand completeCmd = new CompleteReportCommand(
                reportId, 100L, "summary", "data"
            );
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeCmd)))
                .andExpect(status().isOk());

            FailReportCommand failCmd = new FailReportCommand(reportId, "error");
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/fail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failCmd)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/analytics/reports/{id}/cancel")
    class CancelReport {

        @Test
        @DisplayName("should cancel a generating report")
        void shouldCancelGeneratingReport() throws Exception {
            String reportId = createReportAndGetId();

            CancelReportCommand command = new CancelReportCommand(reportId, "No longer needed");

            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("should return 409 when cancelling a completed report")
        void shouldReturn409WhenCancellingCompletedReport() throws Exception {
            String reportId = createReportAndGetId();

            CompleteReportCommand completeCmd = new CompleteReportCommand(
                reportId, 100L, "summary", "data"
            );
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeCmd)))
                .andExpect(status().isOk());

            CancelReportCommand cancelCmd = new CancelReportCommand(reportId, "reason");
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelCmd)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/analytics/reports/{id}/regenerate")
    class RegenerateReport {

        @Test
        @DisplayName("should regenerate a failed report")
        void shouldRegenerateFailedReport() throws Exception {
            String reportId = createReportAndGetId();

            FailReportCommand failCmd = new FailReportCommand(reportId, "error");
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/fail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failCmd)))
                .andExpect(status().isOk());

            RegenerateReportCommand regenCmd = new RegenerateReportCommand(reportId, "admin2");
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/regenerate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(regenCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("GENERATING"))
                .andExpect(jsonPath("$.data.generatedBy").value("admin2"));
        }

        @Test
        @DisplayName("should regenerate a cancelled report")
        void shouldRegenerateCancelledReport() throws Exception {
            String reportId = createReportAndGetId();

            CancelReportCommand cancelCmd = new CancelReportCommand(reportId, "cancelled");
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelCmd)))
                .andExpect(status().isOk());

            RegenerateReportCommand regenCmd = new RegenerateReportCommand(reportId, "admin2");
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/regenerate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(regenCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GENERATING"));
        }

        @Test
        @DisplayName("should return 409 when regenerating a generating report")
        void shouldReturn409WhenRegeneratingGeneratingReport() throws Exception {
            String reportId = createReportAndGetId();

            RegenerateReportCommand regenCmd = new RegenerateReportCommand(reportId, "admin");
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/regenerate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(regenCmd)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Full Lifecycle")
    class FullLifecycle {

        @Test
        @DisplayName("should support full report lifecycle: create -> complete")
        void shouldSupportCreateCompleteLifecycle() throws Exception {
            String reportId = createReportAndGetId();

            mockMvc.perform(get("/api/analytics/reports/" + reportId))
                .andExpect(jsonPath("$.data.status").value("GENERATING"));

            CompleteReportCommand completeCmd = new CompleteReportCommand(
                reportId, 250L, "Full summary", "{full: true}"
            );
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeCmd)))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.totalRecords").value(250));
        }

        @Test
        @DisplayName("should support full report lifecycle: create -> fail -> regenerate -> complete")
        void shouldSupportCreateFailRegenerateCompleteLifecycle() throws Exception {
            String reportId = createReportAndGetId();

            FailReportCommand failCmd = new FailReportCommand(reportId, "timeout");
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/fail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failCmd)))
                .andExpect(jsonPath("$.data.status").value("FAILED"));

            RegenerateReportCommand regenCmd = new RegenerateReportCommand(reportId, "admin2");
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/regenerate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(regenCmd)))
                .andExpect(jsonPath("$.data.status").value("GENERATING"));

            CompleteReportCommand completeCmd = new CompleteReportCommand(
                reportId, 100L, "Regenerated summary", "{data: [...]}"
            );
            mockMvc.perform(post("/api/analytics/reports/" + reportId + "/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeCmd)))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.totalRecords").value(100));
        }
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<ReportDTO> readReportResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ReportDTO.class));
    }
}
