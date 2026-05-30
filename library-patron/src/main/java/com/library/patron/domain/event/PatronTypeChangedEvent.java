package com.library.patron.domain.event;

import com.library.patron.domain.model.enums.PatronType;
import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.PatronId;

public class PatronTypeChangedEvent extends DomainEvent {

    private final PatronId patronId;
    private final PatronType oldType;
    private final PatronType newType;

    public PatronTypeChangedEvent(PatronId patronId, PatronType oldType, PatronType newType) {
        super();
        this.patronId = patronId;
        this.oldType = oldType;
        this.newType = newType;
    }

    public PatronId getPatronId() { return patronId; }
    public PatronType getOldType() { return oldType; }
    public PatronType getNewType() { return newType; }
}
