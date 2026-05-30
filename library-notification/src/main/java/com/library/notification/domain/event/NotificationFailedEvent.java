package com.library.notification.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.NotificationId;

public class NotificationFailedEvent extends DomainEvent {

    private final NotificationId notificationId;
    private final String reason;

    public NotificationFailedEvent(NotificationId notificationId, String reason) {
        super();
        this.notificationId = notificationId;
        this.reason = reason;
    }

    public NotificationId getNotificationId() { return notificationId; }
    public String getReason() { return reason; }
}
