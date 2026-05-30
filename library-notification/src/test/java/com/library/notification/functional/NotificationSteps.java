package com.library.notification.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.application.dto.ApiResponse;
import com.library.notification.application.dto.NotificationDTO;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class NotificationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationScenarioState state;

    @Given("a notification is created for patron {string}")
    public void aNotificationIsCreatedForPatron(String patronId) throws Exception {
        state.setRecipientId(patronId);
        Map<String, Object> body = new HashMap<>();
        body.put("notificationType", "DUE_DATE_REMINDER");
        body.put("channel", "EMAIL");
        body.put("recipientId", patronId);
        body.put("subject", "Test Notification");
        body.put("content", "This is a test notification");

        MvcResult result = mockMvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();

        state.setMvcResult(result);
        ApiResponse<NotificationDTO> response = readNotificationResponse(result);
        if (response.getData() != null) {
            state.setNotificationId(response.getData().getId());
        }
    }

    @When("the notification is sent via {string}")
    public void theNotificationIsSentVia(String channel) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/send", state.getNotificationId()))
            .andReturn();
        state.setMvcResult(result);
    }

    @Then("the notification should be in {string} status")
    public void theNotificationShouldBeInStatus(String expectedStatus) throws Exception {
        MvcResult getResult = mockMvc.perform(get("/api/notifications/{id}", state.getNotificationId()))
            .andReturn();
        ApiResponse<NotificationDTO> response = readNotificationResponse(getResult);
        assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
    }

    @When("the notification is delivered")
    public void theNotificationIsDelivered() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/deliver", state.getNotificationId()))
            .andReturn();
        state.setMvcResult(result);
    }

    @When("the notification is marked as read")
    public void theNotificationIsMarkedAsRead() throws Exception {
        MvcResult result = mockMvc.perform(put("/api/notifications/{id}/read", state.getNotificationId()))
            .andReturn();
        state.setMvcResult(result);
    }

    @When("the notification fails with reason {string}")
    public void theNotificationFailsWithReason(String reason) throws Exception {
        Map<String, String> failBody = Map.of("reason", reason);
        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/fail", state.getNotificationId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(failBody)))
            .andReturn();
        state.setMvcResult(result);
    }

    @When("the notification is retried")
    public void theNotificationIsRetried() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/retry", state.getNotificationId()))
            .andReturn();
        state.setMvcResult(result);
    }

    @When("the notification is scheduled for {int} hours from now")
    public void theNotificationIsScheduledForHoursFromNow(int hours) throws Exception {
        Map<String, String> scheduleBody = new HashMap<>();
        scheduleBody.put("scheduledAt", LocalDateTime.now().plusHours(hours).toString());
        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/schedule", state.getNotificationId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scheduleBody)))
            .andReturn();
        state.setMvcResult(result);
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<NotificationDTO> readNotificationResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, NotificationDTO.class));
    }
}
