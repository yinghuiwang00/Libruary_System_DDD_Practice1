package com.library.notification.application.service;

import com.library.notification.application.command.*;
import com.library.notification.application.dto.NotificationDTO;
import com.library.notification.domain.model.Notification;
import com.library.notification.domain.model.enums.NotificationChannel;
import com.library.notification.domain.model.enums.NotificationPriority;
import com.library.notification.domain.model.enums.NotificationStatus;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.notification.domain.service.NotificationService;
import com.library.shared.domain.model.NotificationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationApplicationService {

    private final NotificationService notificationService;

    public NotificationApplicationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Transactional
    public NotificationDTO createNotification(CreateNotificationCommand command) {
        Notification notification = notificationService.createNotification(
            NotificationType.valueOf(command.getNotificationType()),
            NotificationChannel.valueOf(command.getChannel()),
            command.getRecipientId(),
            command.getRecipientEmail(),
            command.getRecipientPhone(),
            command.getSubject(),
            command.getContent(),
            command.getPriority() != null
                ? NotificationPriority.valueOf(command.getPriority())
                : NotificationPriority.NORMAL
        );
        return NotificationDTO.from(notification);
    }

    @Transactional
    public NotificationDTO scheduleNotification(ScheduleNotificationCommand command) {
        Notification notification = notificationService.scheduleNotification(
            NotificationId.of(command.getNotificationId()),
            command.getScheduledAt()
        );
        return NotificationDTO.from(notification);
    }

    @Transactional
    public NotificationDTO sendNotification(SendNotificationCommand command) {
        Notification notification = notificationService.sendNotification(
            NotificationId.of(command.getNotificationId())
        );
        return NotificationDTO.from(notification);
    }

    @Transactional
    public NotificationDTO markDelivered(MarkDeliveredCommand command) {
        Notification notification = notificationService.markDelivered(
            NotificationId.of(command.getNotificationId())
        );
        return NotificationDTO.from(notification);
    }

    @Transactional
    public NotificationDTO markRead(MarkReadCommand command) {
        Notification notification = notificationService.markRead(
            NotificationId.of(command.getNotificationId())
        );
        return NotificationDTO.from(notification);
    }

    @Transactional
    public NotificationDTO failNotification(FailNotificationCommand command) {
        Notification notification = notificationService.failNotification(
            NotificationId.of(command.getNotificationId()),
            command.getReason()
        );
        return NotificationDTO.from(notification);
    }

    @Transactional
    public NotificationDTO retryNotification(RetryNotificationCommand command) {
        Notification notification = notificationService.retryNotification(
            NotificationId.of(command.getNotificationId())
        );
        return NotificationDTO.from(notification);
    }

    @Transactional
    public NotificationDTO cancelNotification(CancelNotificationCommand command) {
        Notification notification = notificationService.cancelNotification(
            NotificationId.of(command.getNotificationId()),
            command.getReason()
        );
        return NotificationDTO.from(notification);
    }

    @Transactional(readOnly = true)
    public NotificationDTO getNotification(String id) {
        Notification notification = notificationService.getNotification(NotificationId.of(id));
        return NotificationDTO.from(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getAllNotifications() {
        return notificationService.getAllNotifications().stream()
            .map(NotificationDTO::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getByRecipientId(String recipientId) {
        return notificationService.getByRecipientId(recipientId).stream()
            .map(NotificationDTO::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getByStatus(String status) {
        return notificationService.getByStatus(NotificationStatus.valueOf(status)).stream()
            .map(NotificationDTO::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getByType(String type) {
        return notificationService.getByType(NotificationType.valueOf(type)).stream()
            .map(NotificationDTO::from)
            .collect(Collectors.toList());
    }
}
