package com.library.notification.domain.event;

import com.library.notification.domain.model.enums.NotificationType;
import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.NotificationId;

public class NotificationCreatedEvent extends DomainEvent {

    private final NotificationId notificationId;
    private final NotificationType notificationType;
    private final String recipientId;

    public NotificationCreatedEvent(NotificationId notificationId, NotificationType notificationType,
                                     String recipientId) {
        super();
        this.notificationId = notificationId;
        this.notificationType = notificationType;
        this.recipientId = recipientId;
    }

    public NotificationId getNotificationId() { return notificationId; }
    public NotificationType getNotificationType() { return notificationType; }
    public String getRecipientId() { return recipientId; }
}
