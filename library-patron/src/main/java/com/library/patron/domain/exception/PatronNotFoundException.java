package com.library.patron.domain.exception;

import com.library.shared.domain.model.PatronId;

public class PatronNotFoundException extends DomainException {

    public PatronNotFoundException(PatronId patronId) {
        super("PATRON_NOT_FOUND", "Patron not found: " + patronId);
    }
}
