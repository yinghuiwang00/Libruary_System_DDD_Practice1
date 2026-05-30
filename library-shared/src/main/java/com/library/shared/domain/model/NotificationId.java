package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class NotificationId extends AggregateId {

    protected NotificationId() {
    }

    private NotificationId(String value) {
        super(value);
    }

    public static NotificationId generate() {
        return new NotificationId(generateUUID());
    }

    public static NotificationId of(String value) {
        return new NotificationId(value);
    }
}
