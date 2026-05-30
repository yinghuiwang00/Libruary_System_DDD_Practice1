package com.library.notification.application.command;

import jakarta.validation.constraints.NotBlank;

public class SendNotificationCommand {

    @NotBlank(message = "Notification ID must not be blank")
    private String notificationId;

    public SendNotificationCommand() {
    }

    public SendNotificationCommand(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
}
