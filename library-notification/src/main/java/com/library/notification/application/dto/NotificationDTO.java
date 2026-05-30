package com.library.notification.application.dto;

import com.library.notification.domain.model.Notification;

import java.time.LocalDateTime;

public class NotificationDTO {

    private String id;
    private String notificationType;
    private String priority;
    private String channel;
    private String recipientId;
    private String recipientEmail;
    private String recipientPhone;
    private String subject;
    private String content;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private LocalDateTime failedAt;
    private String failureReason;
    private Integer retryCount;
    private Integer maxRetries;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NotificationDTO() {
    }

    public static NotificationDTO from(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.id = notification.getId().getValue();
        dto.notificationType = notification.getNotificationType().name();
        dto.priority = notification.getPriority().name();
        dto.channel = notification.getChannel().name();
        dto.recipientId = notification.getRecipientId();
        dto.recipientEmail = notification.getRecipientEmail();
        dto.recipientPhone = notification.getRecipientPhone();
        dto.subject = notification.getSubject();
        dto.content = notification.getContent();
        dto.status = notification.getStatus().name();
        dto.scheduledAt = notification.getScheduledAt();
        dto.sentAt = notification.getSentAt();
        dto.deliveredAt = notification.getDeliveredAt();
        dto.readAt = notification.getReadAt();
        dto.failedAt = notification.getFailedAt();
        dto.failureReason = notification.getFailureReason();
        dto.retryCount = notification.getRetryCount();
        dto.maxRetries = notification.getMaxRetries();
        dto.version = notification.getVersion();
        dto.createdAt = notification.getCreatedAt();
        dto.updatedAt = notification.getUpdatedAt();
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
