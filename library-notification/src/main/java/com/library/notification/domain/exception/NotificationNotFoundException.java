package com.library.notification.domain.exception;

import com.library.shared.domain.model.NotificationId;

public class NotificationNotFoundException extends DomainException {

    public NotificationNotFoundException(NotificationId notificationId) {
        super("NOTIFICATION_NOT_FOUND", "Notification not found: " + notificationId);
    }
}
