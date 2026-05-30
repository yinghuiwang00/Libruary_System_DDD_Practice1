package com.library.notification.application.command;

import jakarta.validation.constraints.NotBlank;

public class FailNotificationCommand {

    @NotBlank(message = "Notification ID must not be blank")
    private String notificationId;

    @NotBlank(message = "Failure reason must not be blank")
    private String reason;

    public FailNotificationCommand() {
    }

    public FailNotificationCommand(String notificationId, String reason) {
        this.notificationId = notificationId;
        this.reason = reason;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
