package com.library.notification.domain.service;

import com.library.notification.domain.event.NotificationCreatedEvent;
import com.library.notification.domain.exception.InvalidOperationException;
import com.library.notification.domain.exception.NotificationNotFoundException;
import com.library.notification.domain.model.Notification;
import com.library.notification.domain.model.enums.NotificationChannel;
import com.library.notification.domain.model.enums.NotificationStatus;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.notification.domain.repository.NotificationRepository;
import com.library.shared.domain.event.DomainEventPublisher;
import com.library.shared.domain.model.NotificationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private static final NotificationType TYPE = NotificationType.DUE_DATE_REMINDER;
    private static final NotificationChannel CHANNEL = NotificationChannel.EMAIL;
    private static final String RECIPIENT_ID = "patron-001";
    private static final String SUBJECT = "Due Date Reminder";
    private static final String CONTENT = "Your book is due tomorrow.";

    // =========================================================================
    // CreateNotification
    // =========================================================================

    @Nested
    @DisplayName("CreateNotification")
    class CreateNotification {

        @Test
        @DisplayName("should create notification, save, and publish event")
        void shouldCreateAndPublishEvent() {
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Notification result = notificationService.createNotification(TYPE, CHANNEL, RECIPIENT_ID, SUBJECT, CONTENT);

            assertThat(result).isNotNull();
            assertThat(result.getNotificationType()).isEqualTo(TYPE);
            assertThat(result.getChannel()).isEqualTo(CHANNEL);
            assertThat(result.getRecipientId()).isEqualTo(RECIPIENT_ID);
            assertThat(result.getSubject()).isEqualTo(SUBJECT);
            assertThat(result.getContent()).isEqualTo(CONTENT);
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);

            verify(notificationRepository).save(any(Notification.class));

            ArgumentCaptor<NotificationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            NotificationCreatedEvent event = eventCaptor.getValue();
            assertThat(event.getNotificationId()).isEqualTo(result.getId());
            assertThat(event.getNotificationType()).isEqualTo(TYPE);
            assertThat(event.getRecipientId()).isEqualTo(RECIPIENT_ID);
        }

        @Test
        @DisplayName("should return the saved notification")
        void shouldReturnSavedNotification() {
            Notification saved = Notification.create(TYPE, CHANNEL, RECIPIENT_ID, SUBJECT, CONTENT);
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

            Notification result = notificationService.createNotification(TYPE, CHANNEL, RECIPIENT_ID, SUBJECT, CONTENT);

            assertThat(result).isSameAs(saved);
        }

        @Test
        @DisplayName("should validate all fields are set correctly")
        void shouldValidateAllFields() {
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Notification result = notificationService.createNotification(
                NotificationType.OVERDUE_NOTICE, NotificationChannel.SMS, "patron-002", "Overdue", "Book overdue");

            assertThat(result.getNotificationType()).isEqualTo(NotificationType.OVERDUE_NOTICE);
            assertThat(result.getChannel()).isEqualTo(NotificationChannel.SMS);
            assertThat(result.getRecipientId()).isEqualTo("patron-002");
        }
    }

    // =========================================================================
    // ScheduleNotification
    // =========================================================================

    @Nested
    @DisplayName("ScheduleNotification")
    class ScheduleNotification {

        @Test
        @DisplayName("should schedule notification successfully")
        void shouldScheduleNotification() {
            Notification notification = createPendingNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LocalDateTime scheduledAt = LocalDateTime.now().plusHours(2);
            Notification result = notificationService.scheduleNotification(notification.getId(), scheduledAt);

            assertThat(result.getStatus()).isEqualTo(NotificationStatus.SCHEDULED);
            assertThat(result.getScheduledAt()).isEqualTo(scheduledAt);
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should throw when notification not found")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.scheduleNotification(NotificationId.generate(), LocalDateTime.now().plusHours(1)))
                .isInstanceOf(NotificationNotFoundException.class);
        }
    }

    // =========================================================================
    // SendNotification
    // =========================================================================

    @Nested
    @DisplayName("SendNotification")
    class SendNotification {

        @Test
        @DisplayName("should send notification successfully")
        void shouldSendNotification() {
            Notification notification = createPendingNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Notification result = notificationService.sendNotification(notification.getId());

            assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENDING);
            assertThat(result.getSentAt()).isNotNull();
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should throw when notification not found")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.sendNotification(NotificationId.generate()))
                .isInstanceOf(NotificationNotFoundException.class);
        }
    }

    // =========================================================================
    // MarkDelivered
    // =========================================================================

    @Nested
    @DisplayName("MarkDelivered")
    class MarkDelivered {

        @Test
        @DisplayName("should mark notification as delivered")
        void shouldMarkDelivered() {
            Notification notification = createSendingNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Notification result = notificationService.markDelivered(notification.getId());

            assertThat(result.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
            assertThat(result.getDeliveredAt()).isNotNull();
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should throw when notification not found")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markDelivered(NotificationId.generate()))
                .isInstanceOf(NotificationNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when notification in invalid state for delivery")
        void shouldThrowWhenInvalidState() {
            Notification notification = createPendingNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.markDelivered(notification.getId()))
                .isInstanceOf(InvalidOperationException.class);
        }
    }

    // =========================================================================
    // MarkRead
    // =========================================================================

    @Nested
    @DisplayName("MarkRead")
    class MarkRead {

        @Test
        @DisplayName("should mark notification as read")
        void shouldMarkRead() {
            Notification notification = createDeliveredNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Notification result = notificationService.markRead(notification.getId());

            assertThat(result.getStatus()).isEqualTo(NotificationStatus.READ);
            assertThat(result.getReadAt()).isNotNull();
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should throw when notification not found")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markRead(NotificationId.generate()))
                .isInstanceOf(NotificationNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when notification not in DELIVERED state")
        void shouldThrowWhenNotDelivered() {
            Notification notification = createSendingNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.markRead(notification.getId()))
                .isInstanceOf(InvalidOperationException.class);
        }
    }

    // =========================================================================
    // FailNotification
    // =========================================================================

    @Nested
    @DisplayName("FailNotification")
    class FailNotification {

        @Test
        @DisplayName("should fail notification and set failure reason")
        void shouldFailNotification() {
            Notification notification = createSendingNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            String reason = "SMTP timeout";
            Notification result = notificationService.failNotification(notification.getId(), reason);

            assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(result.getFailureReason()).isEqualTo(reason);
            assertThat(result.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should publish event on failure")
        void shouldPublishEventOnFailure() {
            Notification notification = createSendingNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            notificationService.failNotification(notification.getId(), "Network error");

            verify(eventPublisher).publish(any(com.library.shared.domain.event.DomainEvent.class));
        }

        @Test
        @DisplayName("should throw when notification not found")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.failNotification(NotificationId.generate(), "reason"))
                .isInstanceOf(NotificationNotFoundException.class);
        }
    }

    // =========================================================================
    // RetryNotification
    // =========================================================================

    @Nested
    @DisplayName("RetryNotification")
    class RetryNotification {

        @Test
        @DisplayName("should retry failed notification successfully")
        void shouldRetryNotification() {
            Notification notification = createFailedNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Notification result = notificationService.retryNotification(notification.getId());

            assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(result.getFailureReason()).isNull();
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should throw when max retries exceeded")
        void shouldThrowWhenMaxRetriesExceeded() {
            Notification notification = createFailedNotification();
            // Exhaust retries: retryCount=1 -> retry -> send -> fail(retryCount=2) -> retry -> send -> fail(retryCount=3)
            notification.retry();
            notification.send();
            notification.fail("2nd fail");
            notification.retry();
            notification.send();
            notification.fail("3rd fail");

            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.retryNotification(notification.getId()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Maximum retry attempts");
        }

        @Test
        @DisplayName("should throw when notification not found")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.retryNotification(NotificationId.generate()))
                .isInstanceOf(NotificationNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when notification not in FAILED state")
        void shouldThrowWhenNotFailed() {
            Notification notification = createPendingNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.retryNotification(notification.getId()))
                .isInstanceOf(InvalidOperationException.class);
        }
    }

    // =========================================================================
    // CancelNotification
    // =========================================================================

    @Nested
    @DisplayName("CancelNotification")
    class CancelNotification {

        @Test
        @DisplayName("should cancel notification successfully")
        void shouldCancelNotification() {
            Notification notification = createPendingNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            String reason = "No longer needed";
            Notification result = notificationService.cancelNotification(notification.getId(), reason);

            assertThat(result.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
            assertThat(result.getFailureReason()).isEqualTo(reason);
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should throw when notification not found")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.cancelNotification(NotificationId.generate(), "reason"))
                .isInstanceOf(NotificationNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when notification is in terminal state")
        void shouldThrowWhenInTerminalState() {
            Notification notification = createDeliveredNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.cancelNotification(notification.getId(), "reason"))
                .isInstanceOf(InvalidOperationException.class);
        }
    }

    // =========================================================================
    // GetNotification
    // =========================================================================

    @Nested
    @DisplayName("GetNotification")
    class GetNotification {

        @Test
        @DisplayName("should return notification when found")
        void shouldReturnNotificationWhenFound() {
            Notification notification = createPendingNotification();
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(notification));

            Notification result = notificationService.getNotification(notification.getId());

            assertThat(result).isSameAs(notification);
        }

        @Test
        @DisplayName("should throw when notification not found")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.getNotification(NotificationId.generate()))
                .isInstanceOf(NotificationNotFoundException.class);
        }
    }

    // =========================================================================
    // GetAllNotifications
    // =========================================================================

    @Nested
    @DisplayName("GetAllNotifications")
    class GetAllNotifications {

        @Test
        @DisplayName("should return all notifications")
        void shouldReturnAllNotifications() {
            Notification n1 = createPendingNotification();
            Notification n2 = Notification.create(NotificationType.OVERDUE_NOTICE, NotificationChannel.SMS, "patron-002", "Overdue", "Content");
            when(notificationRepository.findAll()).thenReturn(List.of(n1, n2));

            List<Notification> result = notificationService.getAllNotifications();

            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(n1, n2);
        }

        @Test
        @DisplayName("should return empty list when no notifications")
        void shouldReturnEmptyList() {
            when(notificationRepository.findAll()).thenReturn(Collections.emptyList());

            List<Notification> result = notificationService.getAllNotifications();

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GetByRecipientId
    // =========================================================================

    @Nested
    @DisplayName("GetByRecipientId")
    class GetByRecipientId {

        @Test
        @DisplayName("should return notifications filtered by recipient ID")
        void shouldFilterByRecipientId() {
            Notification n1 = createPendingNotification();
            when(notificationRepository.findByRecipientId(RECIPIENT_ID)).thenReturn(List.of(n1));

            List<Notification> result = notificationService.getByRecipientId(RECIPIENT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecipientId()).isEqualTo(RECIPIENT_ID);
        }

        @Test
        @DisplayName("should return empty list when no notifications for recipient")
        void shouldReturnEmptyList() {
            when(notificationRepository.findByRecipientId("unknown")).thenReturn(Collections.emptyList());

            List<Notification> result = notificationService.getByRecipientId("unknown");

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GetByStatus
    // =========================================================================

    @Nested
    @DisplayName("GetByStatus")
    class GetByStatus {

        @Test
        @DisplayName("should return notifications filtered by status")
        void shouldFilterByStatus() {
            Notification n1 = createPendingNotification();
            when(notificationRepository.findByStatus(NotificationStatus.PENDING)).thenReturn(List.of(n1));

            List<Notification> result = notificationService.getByStatus(NotificationStatus.PENDING);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(NotificationStatus.PENDING);
        }

        @Test
        @DisplayName("should return empty list when no notifications with status")
        void shouldReturnEmptyList() {
            when(notificationRepository.findByStatus(NotificationStatus.CANCELLED)).thenReturn(Collections.emptyList());

            List<Notification> result = notificationService.getByStatus(NotificationStatus.CANCELLED);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GetByType
    // =========================================================================

    @Nested
    @DisplayName("GetByType")
    class GetByType {

        @Test
        @DisplayName("should return notifications filtered by type")
        void shouldFilterByType() {
            Notification n1 = createPendingNotification();
            when(notificationRepository.findByNotificationType(TYPE)).thenReturn(List.of(n1));

            List<Notification> result = notificationService.getByType(TYPE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNotificationType()).isEqualTo(TYPE);
        }

        @Test
        @DisplayName("should return empty list when no notifications with type")
        void shouldReturnEmptyList() {
            when(notificationRepository.findByNotificationType(NotificationType.SYSTEM_ANNOUNCEMENT)).thenReturn(Collections.emptyList());

            List<Notification> result = notificationService.getByType(NotificationType.SYSTEM_ANNOUNCEMENT);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Notification createPendingNotification() {
        return Notification.create(TYPE, CHANNEL, RECIPIENT_ID, SUBJECT, CONTENT);
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

    private Notification createFailedNotification() {
        Notification notification = createSendingNotification();
        notification.fail("Test failure");
        return notification;
    }
}
