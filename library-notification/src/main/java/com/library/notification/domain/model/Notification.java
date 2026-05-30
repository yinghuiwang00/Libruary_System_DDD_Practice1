package com.library.notification.domain.model;

import com.library.notification.domain.exception.InvalidOperationException;
import com.library.notification.domain.model.enums.NotificationChannel;
import com.library.notification.domain.model.enums.NotificationPriority;
import com.library.notification.domain.model.enums.NotificationStatus;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.shared.domain.model.NotificationId;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_type", columnList = "notification_type")
})
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    private static final int DEFAULT_MAX_RETRIES = 3;

    @EmbeddedId
    private NotificationId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    @Column(name = "recipient_email", length = 200)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(length = 500)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private NotificationStatus status;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Notification() {
    }

    private Notification(NotificationId id, NotificationType notificationType,
                         NotificationChannel channel, String recipientId,
                         String subject, String content) {
        this.id = Objects.requireNonNull(id, "Notification ID must not be null");
        this.notificationType = Objects.requireNonNull(notificationType, "Notification type must not be null");
        this.channel = Objects.requireNonNull(channel, "Notification channel must not be null");
        this.recipientId = validateRecipientId(recipientId);
        this.subject = validateSubject(subject);
        this.content = validateContent(content);
        this.priority = NotificationPriority.NORMAL;
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.maxRetries = DEFAULT_MAX_RETRIES;
    }

    public static Notification create(NotificationType type, NotificationChannel channel,
                                       String recipientId, String subject, String content) {
        return new Notification(NotificationId.generate(), type, channel, recipientId, subject, content);
    }

    public void schedule(LocalDateTime scheduledAt) {
        if (this.status != NotificationStatus.PENDING) {
            throw new InvalidOperationException(
                "Can only schedule a pending notification. Current status: " + this.status);
        }
        Objects.requireNonNull(scheduledAt, "Scheduled time must not be null");
        this.scheduledAt = scheduledAt;
        this.status = NotificationStatus.SCHEDULED;
    }

    public void send() {
        if (this.status != NotificationStatus.PENDING && this.status != NotificationStatus.SCHEDULED) {
            throw new InvalidOperationException(
                "Can only send a pending or scheduled notification. Current status: " + this.status);
        }
        this.status = NotificationStatus.SENDING;
        this.sentAt = LocalDateTime.now();
    }

    public void markDelivered() {
        if (this.status != NotificationStatus.SENDING) {
            throw new InvalidOperationException(
                "Can only mark a sending notification as delivered. Current status: " + this.status);
        }
        this.status = NotificationStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void markRead() {
        if (this.status != NotificationStatus.DELIVERED) {
            throw new InvalidOperationException(
                "Can only mark a delivered notification as read. Current status: " + this.status);
        }
        this.status = NotificationStatus.READ;
        this.readAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        if (this.status != NotificationStatus.SENDING) {
            throw new InvalidOperationException(
                "Can only fail a sending notification. Current status: " + this.status);
        }
        this.status = NotificationStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.failureReason = reason;
        this.retryCount = this.retryCount + 1;
    }

    public void retry() {
        if (this.status != NotificationStatus.FAILED) {
            throw new InvalidOperationException(
                "Can only retry a failed notification. Current status: " + this.status);
        }
        if (!canBeRetried()) {
            throw new InvalidOperationException(
                "Maximum retry attempts (" + this.maxRetries + ") exceeded");
        }
        this.status = NotificationStatus.PENDING;
        this.failureReason = null;
    }

    public void cancel(String reason) {
        if (isTerminalState()) {
            throw new InvalidOperationException(
                "Cannot cancel a notification in terminal state: " + this.status);
        }
        this.status = NotificationStatus.CANCELLED;
        this.failureReason = reason;
    }

    public boolean isPending() {
        return this.status == NotificationStatus.PENDING;
    }

    public boolean isDelivered() {
        return this.status == NotificationStatus.DELIVERED;
    }

    public boolean isFailed() {
        return this.status == NotificationStatus.FAILED;
    }

    public boolean canBeRetried() {
        return this.retryCount < this.maxRetries;
    }

    public void updateRecipientInfo(String email, String phone) {
        this.recipientEmail = email;
        this.recipientPhone = phone;
    }

    public void setPriority(NotificationPriority priority) {
        if (isTerminalState()) {
            throw new InvalidOperationException(
                "Cannot change priority of a notification in terminal state: " + this.status);
        }
        this.priority = Objects.requireNonNull(priority, "Priority must not be null");
    }

    private boolean isTerminalState() {
        return this.status == NotificationStatus.DELIVERED
            || this.status == NotificationStatus.READ
            || this.status == NotificationStatus.CANCELLED;
    }

    private String validateRecipientId(String recipientId) {
        if (recipientId == null || recipientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient ID must not be empty");
        }
        return recipientId.trim();
    }

    private String validateSubject(String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject must not be empty");
        }
        if (subject.length() > 500) {
            throw new IllegalArgumentException("Subject must not exceed 500 characters");
        }
        return subject.trim();
    }

    private String validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content must not be empty");
        }
        return content.trim();
    }

    // Getters
    public NotificationId getId() { return id; }
    public NotificationType getNotificationType() { return notificationType; }
    public NotificationPriority getPriority() { return priority; }
    public NotificationChannel getChannel() { return channel; }
    public String getRecipientId() { return recipientId; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public NotificationStatus getStatus() { return status; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public String getFailureReason() { return failureReason; }
    public Integer getRetryCount() { return retryCount; }
    public Integer getMaxRetries() { return maxRetries; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
