package com.library.patron.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.PatronId;

public class PatronUpdatedEvent extends DomainEvent {

    private final PatronId patronId;

    public PatronUpdatedEvent(PatronId patronId) {
        super();
        this.patronId = patronId;
    }

    public PatronId getPatronId() { return patronId; }
}
