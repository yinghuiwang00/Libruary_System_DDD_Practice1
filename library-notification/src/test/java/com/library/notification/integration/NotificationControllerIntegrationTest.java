package com.library.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.application.dto.ApiResponse;
import com.library.notification.application.dto.NotificationDTO;
import com.library.notification.domain.model.enums.NotificationChannel;
import com.library.notification.domain.model.enums.NotificationStatus;
import com.library.notification.domain.model.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================================================
    // Create Notification
    // =========================================================================

    @Test
    @DisplayName("POST /api/notifications - should create notification and return 201")
    void createNotification_shouldReturn201() throws Exception {
        Map<String, Object> body = buildCreateBody("patron-001", NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);

        MvcResult result = mockMvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.notificationType").value("DUE_DATE_REMINDER"))
            .andExpect(jsonPath("$.data.channel").value("EMAIL"))
            .andExpect(jsonPath("$.data.recipientId").value("patron-001"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.subject").value("Test Subject"))
            .andExpect(jsonPath("$.data.content").value("Test Content"))
            .andReturn();

        ApiResponse<NotificationDTO> response = readNotificationResponse(result);
        assertThat(response.getData().getId()).isNotBlank();
        assertThat(response.getData().getPriority()).isEqualTo("NORMAL");
        assertThat(response.getData().getRetryCount()).isEqualTo(0);
        assertThat(response.getData().getMaxRetries()).isEqualTo(3);
    }

    // =========================================================================
    // Get All Notifications
    // =========================================================================

    @Test
    @DisplayName("GET /api/notifications - should list all notifications")
    void getAllNotifications_shouldReturnList() throws Exception {
        createNotificationViaApi("patron-001", NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);
        createNotificationViaApi("patron-002", NotificationType.OVERDUE_NOTICE, NotificationChannel.SMS);

        MvcResult result = mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<NotificationDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationDTO.class)));
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(2);
    }

    // =========================================================================
    // Get Notification By ID
    // =========================================================================

    @Test
    @DisplayName("GET /api/notifications/{id} - should return notification")
    void getNotificationById_shouldReturnNotification() throws Exception {
        String notificationId = createNotificationViaApi("patron-001", NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);

        mockMvc.perform(get("/api/notifications/{id}", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(notificationId))
            .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/notifications/{id} - should return 404 when not found")
    void getNotificationById_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/notifications/{id}", "nonexistent-id"))
            .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Schedule Notification
    // =========================================================================

    @Test
    @DisplayName("POST /api/notifications/{id}/schedule - should schedule notification")
    void scheduleNotification_shouldReturn200() throws Exception {
        String notificationId = createNotificationViaApi("patron-001", NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);

        Map<String, String> scheduleBody = new HashMap<>();
        scheduleBody.put("scheduledAt", LocalDateTime.now().plusHours(2).toString());

        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/schedule", notificationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scheduleBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
            .andReturn();

        ApiResponse<NotificationDTO> response = readNotificationResponse(result);
        assertThat(response.getData().getScheduledAt()).isNotNull();
    }

    // =========================================================================
    // Send Notification
    // =========================================================================

    @Test
    @DisplayName("POST /api/notifications/{id}/send - should send notification")
    void sendNotification_shouldReturn200() throws Exception {
        String notificationId = createNotificationViaApi("patron-001", NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);

        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/send", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("SENDING"))
            .andReturn();

        ApiResponse<NotificationDTO> response = readNotificationResponse(result);
        assertThat(response.getData().getSentAt()).isNotNull();
    }

    // =========================================================================
    // Deliver Notification
    // =========================================================================

    @Test
    @DisplayName("POST /api/notifications/{id}/deliver - should mark as delivered")
    void deliverNotification_shouldReturn200() throws Exception {
        String notificationId = createAndSendNotification("patron-001");

        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/deliver", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("DELIVERED"))
            .andReturn();

        ApiResponse<NotificationDTO> response = readNotificationResponse(result);
        assertThat(response.getData().getDeliveredAt()).isNotNull();
    }

    // =========================================================================
    // Mark Read
    // =========================================================================

    @Test
    @DisplayName("PUT /api/notifications/{id}/read - should mark as read")
    void markRead_shouldReturn200() throws Exception {
        String notificationId = createAndDeliverNotification("patron-001");

        MvcResult result = mockMvc.perform(put("/api/notifications/{id}/read", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("READ"))
            .andReturn();

        ApiResponse<NotificationDTO> response = readNotificationResponse(result);
        assertThat(response.getData().getReadAt()).isNotNull();
    }

    // =========================================================================
    // Fail Notification
    // =========================================================================

    @Test
    @DisplayName("POST /api/notifications/{id}/fail - should fail notification")
    void failNotification_shouldReturn200() throws Exception {
        String notificationId = createAndSendNotification("patron-001");

        Map<String, String> failBody = Map.of("reason", "SMTP connection refused");

        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/fail", notificationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(failBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("FAILED"))
            .andReturn();

        ApiResponse<NotificationDTO> response = readNotificationResponse(result);
        assertThat(response.getData().getFailureReason()).isEqualTo("SMTP connection refused");
        assertThat(response.getData().getRetryCount()).isEqualTo(1);
    }

    // =========================================================================
    // Retry Notification
    // =========================================================================

    @Test
    @DisplayName("POST /api/notifications/{id}/retry - should retry notification")
    void retryNotification_shouldReturn200() throws Exception {
        String notificationId = createAndFailNotification("patron-001", "Initial failure");

        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/retry", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn();

        ApiResponse<NotificationDTO> response = readNotificationResponse(result);
        assertThat(response.getData().getFailureReason()).isNull();
    }

    // =========================================================================
    // Cancel Notification
    // =========================================================================

    @Test
    @DisplayName("POST /api/notifications/{id}/cancel - should cancel notification")
    void cancelNotification_shouldReturn200() throws Exception {
        String notificationId = createNotificationViaApi("patron-001", NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);

        Map<String, String> cancelBody = Map.of("reason", "No longer needed");

        MvcResult result = mockMvc.perform(post("/api/notifications/{id}/cancel", notificationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("CANCELLED"))
            .andReturn();

        ApiResponse<NotificationDTO> response = readNotificationResponse(result);
        assertThat(response.getData().getFailureReason()).isEqualTo("No longer needed");
    }

    // =========================================================================
    // Filter by Recipient
    // =========================================================================

    @Test
    @DisplayName("GET /api/notifications?recipientId=X - should filter by recipient")
    void shouldFilterByRecipient() throws Exception {
        createNotificationViaApi("patron-filter", NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);
        createNotificationViaApi("patron-filter", NotificationType.OVERDUE_NOTICE, NotificationChannel.SMS);
        createNotificationViaApi("patron-other", NotificationType.FINE_NOTIFICATION, NotificationChannel.EMAIL);

        MvcResult result = mockMvc.perform(get("/api/notifications")
                .param("recipientId", "patron-filter"))
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<NotificationDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationDTO.class)));
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData()).allMatch(n -> n.getRecipientId().equals("patron-filter"));
    }

    // =========================================================================
    // Filter by Status
    // =========================================================================

    @Test
    @DisplayName("GET /api/notifications?status=PENDING - should filter by status")
    void shouldFilterByStatus() throws Exception {
        createNotificationViaApi("patron-001", NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);
        String notificationId = createAndSendNotification("patron-002");

        MvcResult result = mockMvc.perform(get("/api/notifications")
                .param("status", "PENDING"))
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<NotificationDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationDTO.class)));
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(response.getData()).allMatch(n -> n.getStatus().equals("PENDING"));
    }

    // =========================================================================
    // Filter by Type
    // =========================================================================

    @Test
    @DisplayName("GET /api/notifications?type=DUE_DATE_REMINDER - should filter by type")
    void shouldFilterByType() throws Exception {
        createNotificationViaApi("patron-001", NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);
        createNotificationViaApi("patron-002", NotificationType.OVERDUE_NOTICE, NotificationChannel.SMS);

        MvcResult result = mockMvc.perform(get("/api/notifications")
                .param("type", "DUE_DATE_REMINDER"))
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<NotificationDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationDTO.class)));
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getNotificationType()).isEqualTo("DUE_DATE_REMINDER");
    }

    // =========================================================================
    // Full Lifecycle: schedule -> send -> deliver -> read
    // =========================================================================

    @Test
    @DisplayName("Full lifecycle: create -> schedule -> send -> deliver -> read")
    void shouldCompleteScheduledDeliveryLifecycle() throws Exception {
        String notificationId = createNotificationViaApi("patron-lifecycle", NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);

        // Schedule
        Map<String, String> scheduleBody = new HashMap<>();
        scheduleBody.put("scheduledAt", LocalDateTime.now().plusHours(1).toString());
        mockMvc.perform(post("/api/notifications/{id}/schedule", notificationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scheduleBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        // Send
        mockMvc.perform(post("/api/notifications/{id}/send", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SENDING"));

        // Deliver
        mockMvc.perform(post("/api/notifications/{id}/deliver", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        // Read
        mockMvc.perform(put("/api/notifications/{id}/read", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("READ"));
    }

    // =========================================================================
    // Full Lifecycle: send -> fail -> retry -> send -> deliver
    // =========================================================================

    @Test
    @DisplayName("Full lifecycle: create -> send -> fail -> retry -> send -> deliver")
    void shouldCompleteRetryLifecycle() throws Exception {
        String notificationId = createNotificationViaApi("patron-retry", NotificationType.FINE_NOTIFICATION, NotificationChannel.EMAIL);

        // Send
        mockMvc.perform(post("/api/notifications/{id}/send", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SENDING"));

        // Fail
        Map<String, String> failBody = Map.of("reason", "SMTP error");
        mockMvc.perform(post("/api/notifications/{id}/fail", notificationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(failBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("FAILED"));

        // Retry
        mockMvc.perform(post("/api/notifications/{id}/retry", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));

        // Send again
        mockMvc.perform(post("/api/notifications/{id}/send", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SENDING"));

        // Deliver
        mockMvc.perform(post("/api/notifications/{id}/deliver", notificationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DELIVERED"));
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Map<String, Object> buildCreateBody(String recipientId, NotificationType type, NotificationChannel channel) {
        Map<String, Object> body = new HashMap<>();
        body.put("notificationType", type.name());
        body.put("channel", channel.name());
        body.put("recipientId", recipientId);
        body.put("subject", "Test Subject");
        body.put("content", "Test Content");
        return body;
    }

    private String createNotificationViaApi(String recipientId, NotificationType type, NotificationChannel channel) throws Exception {
        Map<String, Object> body = buildCreateBody(recipientId, type, channel);
        MvcResult result = mockMvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();
        return readNotificationResponse(result).getData().getId();
    }

    private String createAndSendNotification(String recipientId) throws Exception {
        String notificationId = createNotificationViaApi(recipientId, NotificationType.DUE_DATE_REMINDER, NotificationChannel.EMAIL);
        mockMvc.perform(post("/api/notifications/{id}/send", notificationId))
            .andExpect(status().isOk());
        return notificationId;
    }

    private String createAndDeliverNotification(String recipientId) throws Exception {
        String notificationId = createAndSendNotification(recipientId);
        mockMvc.perform(post("/api/notifications/{id}/deliver", notificationId))
            .andExpect(status().isOk());
        return notificationId;
    }

    private String createAndFailNotification(String recipientId, String reason) throws Exception {
        String notificationId = createAndSendNotification(recipientId);
        Map<String, String> failBody = Map.of("reason", reason);
        mockMvc.perform(post("/api/notifications/{id}/fail", notificationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(failBody)))
            .andExpect(status().isOk());
        return notificationId;
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<NotificationDTO> readNotificationResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, NotificationDTO.class));
    }
}
