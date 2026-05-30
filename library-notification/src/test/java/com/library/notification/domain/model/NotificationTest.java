package com.library.notification.domain.model;

import com.library.notification.domain.exception.InvalidOperationException;
import com.library.notification.domain.model.enums.NotificationChannel;
import com.library.notification.domain.model.enums.NotificationPriority;
import com.library.notification.domain.model.enums.NotificationStatus;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.shared.domain.model.NotificationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class NotificationTest {

    private static final NotificationType TYPE = NotificationType.DUE_DATE_REMINDER;
    private static final NotificationChannel CHANNEL = NotificationChannel.EMAIL;
    private static final String RECIPIENT_ID = "patron-001";
    private static final String SUBJECT = "Due Date Reminder";
    private static final String CONTENT = "Your book is due tomorrow.";

    // =========================================================================
    // Creation
    // =========================================================================

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create notification with valid parameters via factory method")
        void shouldCreateNotificationWithValidParameters() {
            Notification notification = Notification.create(TYPE, CHANNEL, RECIPIENT_ID, SUBJECT, CONTENT);

            assertThat(notification).isNotNull();
            assertThat(notification.getId()).isNotNull();
            assertThat(notification.getNotificationType()).isEqualTo(TYPE);
            assertThat(notification.getChannel()).isEqualTo(CHANNEL);
            assertThat(notification.getRecipientId()).isEqualTo(RECIPIENT_ID);
            assertThat(notification.getSubject()).isEqualTo(SUBJECT);
            assertThat(notification.getContent()).isEqualTo(CONTENT);
        }

        @Test
        @DisplayName("should create notification with initial status PENDING")
        void shouldCreateNotificationWithInitialStatusPending() {
            Notification notification = createPendingNotification();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        }

        @Test
        @DisplayName("should create notification with default priority NORMAL")
        void shouldCreateNotificationWithDefaultPriorityNormal() {
            Notification notification = createPendingNotification();

            assertThat(notification.getPriority()).isEqualTo(NotificationPriority.NORMAL);
        }

        @Test
        @DisplayName("should create notification with retryCount 0 and maxRetries 3")
        void shouldCreateNotificationWithDefaultRetrySettings() {
            Notification notification = createPendingNotification();

            assertThat(notification.getRetryCount()).isEqualTo(0);
            assertThat(notification.getMaxRetries()).isEqualTo(3);
        }

        @Test
        @DisplayName("should have null timestamps initially for sentAt, deliveredAt, readAt, failedAt")
        void shouldHaveNullTimestampsInitially() {
            Notification notification = createPendingNotification();

            assertThat(notification.getScheduledAt()).isNull();
            assertThat(notification.getSentAt()).isNull();
            assertThat(notification.getDeliveredAt()).isNull();
            assertThat(notification.getReadAt()).isNull();
            assertThat(notification.getFailedAt()).isNull();
            assertThat(notification.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("should reject null notification type")
        void shouldRejectNullNotificationType() {
            assertThatThrownBy(() -> Notification.create(null, CHANNEL, RECIPIENT_ID, SUBJECT, CONTENT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Notification type must not be null");
        }

        @Test
        @DisplayName("should reject null channel")
        void shouldRejectNullChannel() {
            assertThatThrownBy(() -> Notification.create(TYPE, null, RECIPIENT_ID, SUBJECT, CONTENT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Channel must not be null");
        }

        @Test
        @DisplayName("should reject null recipient ID")
        void shouldRejectNullRecipientId() {
            assertThatThrownBy(() -> Notification.create(TYPE, CHANNEL, null, SUBJECT, CONTENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recipient ID must not be empty");
        }

        @Test
        @DisplayName("should support all notification types")
        void shouldSupportAllNotificationTypes() {
            for (NotificationType type : NotificationType.values()) {
                Notification notification = Notification.create(type, CHANNEL, RECIPIENT_ID, "Subject", "Content");
                assertThat(notification.getNotificationType()).isEqualTo(type);
            }
        }

        @Test
        @DisplayName("should support all notification channels")
        void shouldSupportAllNotificationChannels() {
            for (NotificationChannel channel : NotificationChannel.values()) {
                Notification notification = Notification.create(TYPE, channel, RECIPIENT_ID, "Subject", "Content");
                assertThat(notification.getChannel()).isEqualTo(channel);
            }
        }

        @Test
        @DisplayName("should generate unique IDs for different notifications")
        void shouldGenerateUniqueIds() {
            Notification n1 = createPendingNotification();
            Notification n2 = createPendingNotification();

            assertThat(n1.getId()).isNotEqualTo(n2.getId());
        }
    }

    // =========================================================================
    // Schedule
    // =========================================================================

    @Nested
    @DisplayName("Schedule")
    class Schedule {

        @Test
        @DisplayName("should transition PENDING to SCHEDULED")
        void shouldTransitionPendingToScheduled() {
            Notification notification = createPendingNotification();
            LocalDateTime scheduledAt = LocalDateTime.now().plusHours(1);

            notification.schedule(scheduledAt);

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SCHEDULED);
            assertThat(notification.getScheduledAt()).isEqualTo(scheduledAt);
        }

        @Test
        @DisplayName("should reject schedule when not PENDING")
        void shouldRejectScheduleWhenNotPending() {
            Notification notification = createSendingNotification();

            assertThatThrownBy(() -> notification.schedule(LocalDateTime.now().plusHours(1)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("pending notification");
        }

        @Test
        @DisplayName("should reject schedule when already SCHEDULED")
        void shouldRejectScheduleWhenAlreadyScheduled() {
            Notification notification = createScheduledNotification();

            assertThatThrownBy(() -> notification.schedule(LocalDateTime.now().plusHours(2)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("pending notification");
        }

        @Test
        @DisplayName("should reject schedule when DELIVERED")
        void shouldRejectScheduleWhenDelivered() {
            Notification notification = createDeliveredNotification();

            assertThatThrownBy(() -> notification.schedule(LocalDateTime.now().plusHours(1)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("pending notification");
        }
    }

    // =========================================================================
    // Send
    // =========================================================================

    @Nested
    @DisplayName("Send")
    class Send {

        @Test
        @DisplayName("should transition PENDING to SENDING and set sentAt")
        void shouldTransitionPendingToSending() {
            Notification notification = createPendingNotification();

            notification.send();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENDING);
            assertThat(notification.getSentAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition SCHEDULED to SENDING and set sentAt")
        void shouldTransitionScheduledToSending() {
            Notification notification = createScheduledNotification();

            notification.send();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENDING);
            assertThat(notification.getSentAt()).isNotNull();
        }

        @Test
        @DisplayName("should reject send when already SENDING")
        void shouldRejectSendWhenAlreadySending() {
            Notification notification = createSendingNotification();

            assertThatThrownBy(notification::send)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("pending or scheduled notification");
        }

        @Test
        @DisplayName("should reject send when DELIVERED")
        void shouldRejectSendWhenDelivered() {
            Notification notification = createDeliveredNotification();

            assertThatThrownBy(notification::send)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("pending or scheduled notification");
        }
    }

    // =========================================================================
    // MarkDelivered
    // =========================================================================

    @Nested
    @DisplayName("MarkDelivered")
    class MarkDelivered {

        @Test
        @DisplayName("should transition SENDING to DELIVERED and set deliveredAt")
        void shouldTransitionSendingToDelivered() {
            Notification notification = createSendingNotification();

            notification.markDelivered();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
            assertThat(notification.getDeliveredAt()).isNotNull();
        }

        @Test
        @DisplayName("should reject markDelivered when PENDING")
        void shouldRejectMarkDeliveredWhenPending() {
            Notification notification = createPendingNotification();

            assertThatThrownBy(notification::markDelivered)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("sending notification as delivered");
        }

        @Test
        @DisplayName("should reject markDelivered when already DELIVERED")
        void shouldRejectMarkDeliveredWhenAlreadyDelivered() {
            Notification notification = createDeliveredNotification();

            assertThatThrownBy(notification::markDelivered)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("sending notification as delivered");
        }
    }

    // =========================================================================
    // MarkRead
    // =========================================================================

    @Nested
    @DisplayName("MarkRead")
    class MarkRead {

        @Test
        @DisplayName("should transition DELIVERED to READ and set readAt")
        void shouldTransitionDeliveredToRead() {
            Notification notification = createDeliveredNotification();

            notification.markRead();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
            assertThat(notification.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("should reject markRead when not DELIVERED")
        void shouldRejectMarkReadWhenNotDelivered() {
            Notification notification = createSendingNotification();

            assertThatThrownBy(notification::markRead)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("delivered notification as read");
        }

        @Test
        @DisplayName("should reject markRead when already READ")
        void shouldRejectMarkReadWhenAlreadyRead() {
            Notification notification = createReadNotification();

            assertThatThrownBy(notification::markRead)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("delivered notification as read");
        }
    }

    // =========================================================================
    // Fail
    // =========================================================================

    @Nested
    @DisplayName("Fail")
    class Fail {

        @Test
        @DisplayName("should transition SENDING to FAILED and set failedAt and failureReason")
        void shouldTransitionSendingToFailed() {
            Notification notification = createSendingNotification();
            String reason = "SMTP connection timeout";

            notification.fail(reason);

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(notification.getFailedAt()).isNotNull();
            assertThat(notification.getFailureReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("should increment retryCount on failure")
        void shouldIncrementRetryCountOnFailure() {
            Notification notification = createSendingNotification();

            assertThat(notification.getRetryCount()).isEqualTo(0);

            notification.fail("First failure");

            assertThat(notification.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should reject fail when not SENDING")
        void shouldRejectFailWhenNotSending() {
            Notification notification = createPendingNotification();

            assertThatThrownBy(() -> notification.fail("some reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("sending notification");
        }

        @Test
        @DisplayName("should reject fail when DELIVERED")
        void shouldRejectFailWhenDelivered() {
            Notification notification = createDeliveredNotification();

            assertThatThrownBy(() -> notification.fail("some reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("sending notification");
        }
    }

    // =========================================================================
    // Retry
    // =========================================================================

    @Nested
    @DisplayName("Retry")
    class Retry {

        @Test
        @DisplayName("should transition FAILED to PENDING and clear failureReason")
        void shouldTransitionFailedToPending() {
            Notification notification = createFailedNotification();

            notification.retry();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(notification.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("should throw when max retries exceeded")
        void shouldThrowWhenMaxRetriesExceeded() {
            Notification notification = createFailedNotification();
            // retryCount is already 1 from fail, retry to get to 2, then to 3
            notification.retry();
            notification.send();
            notification.fail("Second failure");
            notification.retry();
            notification.send();
            notification.fail("Third failure");

            // retryCount is now 3, which equals maxRetries
            assertThatThrownBy(notification::retry)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Maximum retry attempts");
        }

        @Test
        @DisplayName("should allow retry when retryCount < maxRetries")
        void shouldAllowRetryWhenUnderMaxRetries() {
            Notification notification = createFailedNotification();

            assertThat(notification.canBeRetried()).isTrue();

            notification.retry();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        }

        @Test
        @DisplayName("should reject retry when not FAILED")
        void shouldRejectRetryWhenNotFailed() {
            Notification notification = createPendingNotification();

            assertThatThrownBy(notification::retry)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("failed notification");
        }

        @Test
        @DisplayName("should reject retry when SENDING")
        void shouldRejectRetryWhenSending() {
            Notification notification = createSendingNotification();

            assertThatThrownBy(notification::retry)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("failed notification");
        }
    }

    // =========================================================================
    // Cancel
    // =========================================================================

    @Nested
    @DisplayName("Cancel")
    class Cancel {

        @Test
        @DisplayName("should cancel from PENDING state")
        void shouldCancelFromPending() {
            Notification notification = createPendingNotification();

            notification.cancel("No longer needed");

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
            assertThat(notification.getFailureReason()).isEqualTo("No longer needed");
        }

        @Test
        @DisplayName("should cancel from SCHEDULED state")
        void shouldCancelFromScheduled() {
            Notification notification = createScheduledNotification();

            notification.cancel("Cancelled by admin");

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
            assertThat(notification.getFailureReason()).isEqualTo("Cancelled by admin");
        }

        @Test
        @DisplayName("should cancel from SENDING state")
        void shouldCancelFromSending() {
            Notification notification = createSendingNotification();

            notification.cancel("Aborted");

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
        }

        @Test
        @DisplayName("should reject cancel from DELIVERED state")
        void shouldRejectCancelFromDelivered() {
            Notification notification = createDeliveredNotification();

            assertThatThrownBy(() -> notification.cancel("reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot cancel notification in status: DELIVERED");
        }

        @Test
        @DisplayName("should reject cancel from READ state")
        void shouldRejectCancelFromRead() {
            Notification notification = createReadNotification();

            assertThatThrownBy(() -> notification.cancel("reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot cancel notification in status: READ");
        }

        @Test
        @DisplayName("should cancel from FAILED state")
        void shouldCancelFromFailed() {
            Notification notification = createFailedNotification();

            notification.cancel("Giving up");

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
            assertThat(notification.getFailureReason()).isEqualTo("Giving up");
        }

        @Test
        @DisplayName("should reject cancel from CANCELLED state")
        void shouldRejectCancelFromCancelled() {
            Notification notification = createPendingNotification();
            notification.cancel("First cancel");

            assertThatThrownBy(() -> notification.cancel("Second cancel"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("terminal state");
        }
    }

    // =========================================================================
    // InvalidTransitions
    // =========================================================================

    @Nested
    @DisplayName("Invalid Transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("should reject markDelivered from PENDING")
        void shouldRejectMarkDeliveredFromPending() {
            Notification notification = createPendingNotification();

            assertThatThrownBy(notification::markDelivered)
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should reject markRead from PENDING")
        void shouldRejectMarkReadFromPending() {
            Notification notification = createPendingNotification();

            assertThatThrownBy(notification::markRead)
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should reject markRead from SENDING")
        void shouldRejectMarkReadFromSending() {
            Notification notification = createSendingNotification();

            assertThatThrownBy(notification::markRead)
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should reject fail from CANCELLED")
        void shouldRejectFailFromCancelled() {
            Notification notification = createPendingNotification();
            notification.cancel("reason");

            assertThatThrownBy(() -> notification.fail("another reason"))
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should reject send from DELIVERED")
        void shouldRejectSendFromDelivered() {
            Notification notification = createDeliveredNotification();

            assertThatThrownBy(notification::send)
                .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should reject send from FAILED")
        void shouldRejectSendFromFailed() {
            Notification notification = createFailedNotification();

            assertThatThrownBy(notification::send)
                .isInstanceOf(InvalidOperationException.class);
        }
    }

    // =========================================================================
    // StatusChecks
    // =========================================================================

    @Nested
    @DisplayName("Status Checks")
    class StatusChecks {

        @Test
        @DisplayName("isPending returns true only for PENDING status")
        void isPendingReturnsTrueOnlyForPending() {
            Notification notification = createPendingNotification();
            assertThat(notification.isPending()).isTrue();

            notification.send();
            assertThat(notification.isPending()).isFalse();
        }

        @Test
        @DisplayName("isDelivered returns true only for DELIVERED status")
        void isDeliveredReturnsTrueOnlyForDelivered() {
            Notification notification = createDeliveredNotification();
            assertThat(notification.isDelivered()).isTrue();

            Notification pending = createPendingNotification();
            assertThat(pending.isDelivered()).isFalse();
        }

        @Test
        @DisplayName("isFailed returns true only for FAILED status")
        void isFailedReturnsTrueOnlyForFailed() {
            Notification notification = createFailedNotification();
            assertThat(notification.isFailed()).isTrue();

            Notification pending = createPendingNotification();
            assertThat(pending.isFailed()).isFalse();
        }

        @Test
        @DisplayName("canBeRetried returns true when retryCount < maxRetries")
        void canBeRetriedReturnsTrueWhenUnderMaxRetries() {
            Notification notification = createFailedNotification();
            assertThat(notification.canBeRetried()).isTrue();
        }

        @Test
        @DisplayName("canBeRetried returns false when retryCount >= maxRetries")
        void canBeRetriedReturnsFalseWhenMaxRetriesReached() {
            Notification notification = createFailedNotification();
            // Exhaust all retries: already at retryCount=1
            notification.retry(); // retryCount still 1, status PENDING
            notification.send();
            notification.fail("2nd failure"); // retryCount=2
            notification.retry();
            notification.send();
            notification.fail("3rd failure"); // retryCount=3

            assertThat(notification.canBeRetried()).isFalse();
        }

        @Test
        @DisplayName("canBeRetried returns true for PENDING notification with retryCount under max")
        void canBeRetriedReturnsTrueForPendingWithRoom() {
            Notification notification = createPendingNotification();
            assertThat(notification.canBeRetried()).isTrue();
        }
    }

    // =========================================================================
    // Full Lifecycle
    // =========================================================================

    @Nested
    @DisplayName("Full Lifecycle")
    class FullLifecycle {

        @Test
        @DisplayName("should support PENDING -> SCHEDULED -> SENDING -> DELIVERED -> READ")
        void shouldSupportScheduledDeliveryLifecycle() {
            Notification notification = createPendingNotification();

            notification.schedule(LocalDateTime.now().plusHours(1));
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SCHEDULED);

            notification.send();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENDING);

            notification.markDelivered();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);

            notification.markRead();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
        }

        @Test
        @DisplayName("should support PENDING -> SENDING -> DELIVERED -> READ")
        void shouldSupportDirectDeliveryLifecycle() {
            Notification notification = createPendingNotification();

            notification.send();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENDING);

            notification.markDelivered();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);

            notification.markRead();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
        }

        @Test
        @DisplayName("should support PENDING -> SENDING -> FAILED -> retry -> SENDING -> DELIVERED")
        void shouldSupportRetryLifecycle() {
            Notification notification = createPendingNotification();

            notification.send();
            notification.fail("SMTP error");
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);

            notification.retry();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);

            notification.send();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENDING);

            notification.markDelivered();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        }

        @Test
        @DisplayName("should support PENDING -> CANCELLED")
        void shouldSupportCancelledLifecycle() {
            Notification notification = createPendingNotification();

            notification.cancel("Not needed");
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Notification createPendingNotification() {
        return Notification.create(TYPE, CHANNEL, RECIPIENT_ID, SUBJECT, CONTENT);
    }

    private Notification createScheduledNotification() {
        Notification notification = createPendingNotification();
        notification.schedule(LocalDateTime.now().plusHours(1));
        return notification;
    }

    private Notification createSendingNotification() {
        Notification notification = createPendingNotification();
        notification.send();
        return notification;
    }

    private Notification createDeliveredNotification() {
        Notification notification = createSendingNotification();
        notification.markDelivered();
        return notification;
    }

    private Notification createReadNotification() {
        Notification notification = createDeliveredNotification();
        notification.markRead();
        return notification;
    }

    private Notification createFailedNotification() {
        Notification notification = createSendingNotification();
        notification.fail("Some failure reason");
        return notification;
    }
}
