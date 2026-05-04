package com.library.catalog.domain.exception;

public class DuplicateAuthorException extends DomainException {
    public DuplicateAuthorException(String message) {
        super("DUPLICATEAUTHOR", message);
    }
}
