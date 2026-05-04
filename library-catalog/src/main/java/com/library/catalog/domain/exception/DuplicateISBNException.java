package com.library.catalog.domain.exception;

public class DuplicateISBNException extends DomainException {
    public DuplicateISBNException(String message) {
        super("DUPLICATE_ISBN", message);
    }
}
