package com.library.notification.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ScheduleNotificationCommand {

    @NotBlank(message = "Notification ID must not be blank")
    private String notificationId;

    @NotNull(message = "Scheduled time must not be null")
    private LocalDateTime scheduledAt;

    public ScheduleNotificationCommand() {
    }

    public ScheduleNotificationCommand(String notificationId, LocalDateTime scheduledAt) {
        this.notificationId = notificationId;
        this.scheduledAt = scheduledAt;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
}
