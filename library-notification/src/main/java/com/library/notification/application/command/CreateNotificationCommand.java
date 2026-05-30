package com.library.notification.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateNotificationCommand {

    @NotNull(message = "Notification type must not be null")
    private String notificationType;

    @NotNull(message = "Notification channel must not be null")
    private String channel;

    @NotBlank(message = "Recipient ID must not be blank")
    private String recipientId;

    private String recipientEmail;

    private String recipientPhone;

    @NotBlank(message = "Subject must not be blank")
    private String subject;

    @NotBlank(message = "Content must not be blank")
    private String content;

    private String priority;

    public CreateNotificationCommand() {
    }

    public CreateNotificationCommand(String notificationType, String channel,
                                     String recipientId, String subject, String content) {
        this.notificationType = notificationType;
        this.channel = channel;
        this.recipientId = recipientId;
        this.subject = subject;
        this.content = content;
    }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
