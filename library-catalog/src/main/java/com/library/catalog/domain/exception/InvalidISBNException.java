package com.library.catalog.domain.exception;

public class InvalidISBNException extends DomainException {
    public InvalidISBNException(String message) {
        super("INVALID_ISBN", message);
    }
}
