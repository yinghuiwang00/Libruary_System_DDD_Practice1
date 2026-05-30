package com.library.patron.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.PatronId;

public class PatronSuspendedEvent extends DomainEvent {

    private final PatronId patronId;
    private final String reason;

    public PatronSuspendedEvent(PatronId patronId, String reason) {
        super();
        this.patronId = patronId;
        this.reason = reason;
    }

    public PatronId getPatronId() { return patronId; }
    public String getReason() { return reason; }
}
