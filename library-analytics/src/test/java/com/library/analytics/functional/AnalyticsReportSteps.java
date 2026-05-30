package com.library.analytics.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.analytics.application.command.*;
import com.library.analytics.application.dto.ApiResponse;
import com.library.analytics.application.dto.ReportDTO;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class AnalyticsReportSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnalyticsScenarioState state;

    @Given("the analytics service is available")
    public void analyticsServiceIsAvailable() {
        // Service is started by Spring Boot test context
    }

    @When("I request a {word} for {word} period")
    public void requestReport(String reportType, String reportPeriod) throws Exception {
        CreateReportCommand command = new CreateReportCommand(
            reportType,
            reportPeriod,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31),
            "admin"
        );

        MvcResult result = mockMvc.perform(post("/api/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn();

        state.setMvcResult(result);
        ApiResponse<ReportDTO> response = readReportResponse(result);
        if (response.getData() != null) {
            state.setCurrentReportId(response.getData().getId());
        }
    }

    @Then("the report should be in {word} status")
    public void reportShouldBeInStatus(String expectedStatus) throws Exception {
        ApiResponse<ReportDTO> response = readReportResponse(state.getMvcResult());
        assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
    }

    @When("I complete the report with {int} records")
    public void completeReportWithRecords(int totalRecords) throws Exception {
        CompleteReportCommand command = new CompleteReportCommand(
            state.getCurrentReportId(),
            (long) totalRecords,
            "Summary data",
            "{records: [...]}"
        );

        MvcResult result = mockMvc.perform(post("/api/analytics/reports/"
                + state.getCurrentReportId() + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn();

        state.setMvcResult(result);
    }

    @Then("the report should have {int} total records")
    public void reportShouldHaveTotalRecords(int expectedRecords) throws Exception {
        ApiResponse<ReportDTO> response = readReportResponse(state.getMvcResult());
        assertThat(response.getData().getTotalRecords()).isEqualTo(expectedRecords);
    }

    @When("I fail the report with error {string}")
    public void failReportWithError(String errorMessage) throws Exception {
        FailReportCommand command = new FailReportCommand(
            state.getCurrentReportId(), errorMessage
        );

        MvcResult result = mockMvc.perform(post("/api/analytics/reports/"
                + state.getCurrentReportId() + "/fail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn();

        state.setMvcResult(result);
    }

    @When("I cancel the report with reason {string}")
    public void cancelReportWithReason(String reason) throws Exception {
        CancelReportCommand command = new CancelReportCommand(
            state.getCurrentReportId(), reason
        );

        MvcResult result = mockMvc.perform(post("/api/analytics/reports/"
                + state.getCurrentReportId() + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn();

        state.setMvcResult(result);
    }

    @When("I regenerate the report as {string}")
    public void regenerateReportAs(String regeneratedBy) throws Exception {
        RegenerateReportCommand command = new RegenerateReportCommand(
            state.getCurrentReportId(), regeneratedBy
        );

        MvcResult result = mockMvc.perform(post("/api/analytics/reports/"
                + state.getCurrentReportId() + "/regenerate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn();

        state.setMvcResult(result);
    }

    @When("I list all reports")
    public void listAllReports() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/analytics/reports"))
            .andReturn();
        state.setMvcResult(result);
    }

    @Then("I should see {int} report(s)")
    public void shouldSeeReports(int expectedCount) throws Exception {
        String json = state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<ReportDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, ReportDTO.class)
            ));
        assertThat(response.getData()).hasSize(expectedCount);
    }

    @When("I filter reports by type {word}")
    public void filterReportsByType(String reportType) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/analytics/reports")
                .param("type", reportType))
            .andReturn();
        state.setMvcResult(result);
    }

    @Then("the report type should be {word}")
    public void reportTypeShouldBe(String expectedType) throws Exception {
        String json = state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<ReportDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, ReportDTO.class)
            ));
        assertThat(response.getData()).isNotEmpty();
        assertThat(response.getData().get(0).getReportType()).isEqualTo(expectedType);
    }

    @When("I filter reports by status {word}")
    public void filterReportsByStatus(String status) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/analytics/reports")
                .param("status", status))
            .andReturn();
        state.setMvcResult(result);
    }

    @Then("the report status should be {word}")
    public void reportStatusShouldBe(String expectedStatus) throws Exception {
        String json = state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<ReportDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, ReportDTO.class)
            ));
        assertThat(response.getData()).isNotEmpty();
        assertThat(response.getData().get(0).getStatus()).isEqualTo(expectedStatus);
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<ReportDTO> readReportResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ReportDTO.class));
    }
}
