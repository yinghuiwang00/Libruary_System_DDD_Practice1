package com.library.patron.domain.event;

import com.library.patron.domain.model.enums.PatronType;
import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.PatronId;

import java.time.LocalDate;

public class PatronRegisteredEvent extends DomainEvent {

    private final PatronId patronId;
    private final String fullName;
    private final String email;
    private final PatronType patronType;
    private final LocalDate memberSince;

    public PatronRegisteredEvent(PatronId patronId, String fullName, String email,
                                  PatronType patronType, LocalDate memberSince) {
        super();
        this.patronId = patronId;
        this.fullName = fullName;
        this.email = email;
        this.patronType = patronType;
        this.memberSince = memberSince;
    }

    public PatronId getPatronId() { return patronId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public PatronType getPatronType() { return patronType; }
    public LocalDate getMemberSince() { return memberSince; }
}
