package com.library.notification.application.command;

import jakarta.validation.constraints.NotBlank;

public class MarkDeliveredCommand {

    @NotBlank(message = "Notification ID must not be blank")
    private String notificationId;

    public MarkDeliveredCommand() {
    }

    public MarkDeliveredCommand(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
}
