package com.library.patron.domain.exception;

public class DuplicateEmailException extends DomainException {

    public DuplicateEmailException(String email) {
        super("DUPLICATE_EMAIL", "Patron with email already exists: " + email);
    }
}
