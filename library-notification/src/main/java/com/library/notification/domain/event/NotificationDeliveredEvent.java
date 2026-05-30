package com.library.notification.domain.event;

import com.library.notification.domain.model.enums.NotificationChannel;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.NotificationId;

public class NotificationDeliveredEvent extends DomainEvent {

    private final NotificationId notificationId;
    private final NotificationType notificationType;
    private final NotificationChannel channel;

    public NotificationDeliveredEvent(NotificationId notificationId, NotificationType notificationType,
                                       NotificationChannel channel) {
        super();
        this.notificationId = notificationId;
        this.notificationType = notificationType;
        this.channel = channel;
    }

    public NotificationId getNotificationId() { return notificationId; }
    public NotificationType getNotificationType() { return notificationType; }
    public NotificationChannel getChannel() { return channel; }
}
