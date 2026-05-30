package com.library.notification.domain.service;

import com.library.notification.domain.event.NotificationCreatedEvent;
import com.library.notification.domain.event.NotificationDeliveredEvent;
import com.library.notification.domain.event.NotificationFailedEvent;
import com.library.notification.domain.event.NotificationReadEvent;
import com.library.notification.domain.exception.NotificationNotFoundException;
import com.library.notification.domain.model.Notification;
import com.library.notification.domain.model.enums.NotificationChannel;
import com.library.notification.domain.model.enums.NotificationStatus;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.notification.domain.repository.NotificationRepository;
import com.library.shared.domain.event.DomainEventPublisher;
import com.library.shared.domain.model.NotificationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DomainEventPublisher eventPublisher;

    public NotificationService(NotificationRepository notificationRepository,
                               DomainEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Notification createNotification(NotificationType type, NotificationChannel channel,
                                            String recipientId, String subject, String content) {
        Notification notification = Notification.create(type, channel, recipientId, subject, content);
        Notification saved = notificationRepository.save(notification);

        eventPublisher.publish(new NotificationCreatedEvent(
            saved.getId(), saved.getNotificationType(), saved.getRecipientId()
        ));
        return saved;
    }

    @Transactional
    public Notification scheduleNotification(NotificationId id, LocalDateTime scheduledAt) {
        Notification notification = findOrThrow(id);
        notification.schedule(scheduledAt);
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification sendNotification(NotificationId id) {
        Notification notification = findOrThrow(id);
        notification.send();
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markDelivered(NotificationId id) {
        Notification notification = findOrThrow(id);
        notification.markDelivered();
        Notification saved = notificationRepository.save(notification);

        eventPublisher.publish(new NotificationDeliveredEvent(
            saved.getId(), saved.getNotificationType(), saved.getChannel()
        ));
        return saved;
    }

    @Transactional
    public Notification markRead(NotificationId id) {
        Notification notification = findOrThrow(id);
        notification.markRead();
        Notification saved = notificationRepository.save(notification);

        eventPublisher.publish(new NotificationReadEvent(saved.getId()));
        return saved;
    }

    @Transactional
    public Notification failNotification(NotificationId id, String reason) {
        Notification notification = findOrThrow(id);
        notification.fail(reason);
        Notification saved = notificationRepository.save(notification);

        eventPublisher.publish(new NotificationFailedEvent(saved.getId(), reason));
        return saved;
    }

    @Transactional
    public Notification retryNotification(NotificationId id) {
        Notification notification = findOrThrow(id);
        notification.retry();
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification cancelNotification(NotificationId id, String reason) {
        Notification notification = findOrThrow(id);
        notification.cancel(reason);
        return notificationRepository.save(notification);
    }

    public Notification getNotification(NotificationId id) {
        return findOrThrow(id);
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public List<Notification> getByRecipientId(String recipientId) {
        return notificationRepository.findByRecipientId(recipientId);
    }

    public List<Notification> getByStatus(NotificationStatus status) {
        return notificationRepository.findByStatus(status);
    }

    public List<Notification> getByType(NotificationType type) {
        return notificationRepository.findByNotificationType(type);
    }

    private Notification findOrThrow(NotificationId id) {
        return notificationRepository.findById(id)
            .orElseThrow(() -> new NotificationNotFoundException(id));
    }
}
