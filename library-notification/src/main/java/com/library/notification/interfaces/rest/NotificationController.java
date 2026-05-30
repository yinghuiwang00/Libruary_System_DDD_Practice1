package com.library.notification.interfaces.rest;

import com.library.notification.application.command.*;
import com.library.notification.application.dto.ApiResponse;
import com.library.notification.application.dto.NotificationDTO;
import com.library.notification.application.service.NotificationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification Management", description = "APIs for managing notifications")
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @PostMapping
    @Operation(summary = "Create a new notification")
    public ResponseEntity<ApiResponse<NotificationDTO>> createNotification(
            @Valid @RequestBody CreateNotificationCommand command) {
        NotificationDTO notification = notificationApplicationService.createNotification(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(notification));
    }

    @GetMapping
    @Operation(summary = "Get all notifications or filter by criteria")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotifications(
            @Parameter(description = "Filter by recipient ID")
            @RequestParam(required = false) String recipientId,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by type")
            @RequestParam(required = false) String type) {
        List<NotificationDTO> notifications;
        if (recipientId != null) {
            notifications = notificationApplicationService.getByRecipientId(recipientId);
        } else if (status != null) {
            notifications = notificationApplicationService.getByStatus(status);
        } else if (type != null) {
            notifications = notificationApplicationService.getByType(type);
        } else {
            notifications = notificationApplicationService.getAllNotifications();
        }
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<ApiResponse<NotificationDTO>> getNotification(
            @PathVariable String id) {
        NotificationDTO notification = notificationApplicationService.getNotification(id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PostMapping("/{id}/schedule")
    @Operation(summary = "Schedule a notification for future delivery")
    public ResponseEntity<ApiResponse<NotificationDTO>> scheduleNotification(
            @PathVariable String id,
            @RequestBody ScheduleRequest request) {
        ScheduleNotificationCommand command = new ScheduleNotificationCommand(id, request.scheduledAt());
        NotificationDTO notification = notificationApplicationService.scheduleNotification(command);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Send a notification")
    public ResponseEntity<ApiResponse<NotificationDTO>> sendNotification(
            @PathVariable String id) {
        SendNotificationCommand command = new SendNotificationCommand(id);
        NotificationDTO notification = notificationApplicationService.sendNotification(command);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PostMapping("/{id}/deliver")
    @Operation(summary = "Mark a notification as delivered")
    public ResponseEntity<ApiResponse<NotificationDTO>> markDelivered(
            @PathVariable String id) {
        MarkDeliveredCommand command = new MarkDeliveredCommand(id);
        NotificationDTO notification = notificationApplicationService.markDelivered(command);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationDTO>> markRead(
            @PathVariable String id) {
        MarkReadCommand command = new MarkReadCommand(id);
        NotificationDTO notification = notificationApplicationService.markRead(command);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "Mark a notification as failed")
    public ResponseEntity<ApiResponse<NotificationDTO>> failNotification(
            @PathVariable String id,
            @RequestBody FailRequest request) {
        FailNotificationCommand command = new FailNotificationCommand(id, request.reason());
        NotificationDTO notification = notificationApplicationService.failNotification(command);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed notification")
    public ResponseEntity<ApiResponse<NotificationDTO>> retryNotification(
            @PathVariable String id) {
        RetryNotificationCommand command = new RetryNotificationCommand(id);
        NotificationDTO notification = notificationApplicationService.retryNotification(command);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a notification")
    public ResponseEntity<ApiResponse<NotificationDTO>> cancelNotification(
            @PathVariable String id,
            @RequestBody CancelRequest request) {
        CancelNotificationCommand command = new CancelNotificationCommand(id, request.reason());
        NotificationDTO notification = notificationApplicationService.cancelNotification(command);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    /**
     * Request body records for endpoint-specific payloads.
     */
    public record ScheduleRequest(LocalDateTime scheduledAt) {}
    public record FailRequest(String reason) {}
    public record CancelRequest(String reason) {}
}
