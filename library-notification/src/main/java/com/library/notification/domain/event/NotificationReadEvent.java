package com.library.notification.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.NotificationId;

public class NotificationReadEvent extends DomainEvent {

    private final NotificationId notificationId;

    public NotificationReadEvent(NotificationId notificationId) {
        super();
        this.notificationId = notificationId;
    }

    public NotificationId getNotificationId() { return notificationId; }
}
