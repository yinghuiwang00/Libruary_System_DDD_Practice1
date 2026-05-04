package com.library.shared.domain.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class DomainEvent implements Serializable {

    private final String eventId;
    private final LocalDateTime occurredAt;
    private final String eventType;
    private final int version;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
        this.version = 1;
    }

    protected DomainEvent(String eventId, LocalDateTime occurredAt, int version) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.eventType = this.getClass().getSimpleName();
        this.version = version;
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEvent that = (DomainEvent) o;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return eventType + "{eventId='" + eventId + "', occurredAt=" + occurredAt + '}';
    }
}
