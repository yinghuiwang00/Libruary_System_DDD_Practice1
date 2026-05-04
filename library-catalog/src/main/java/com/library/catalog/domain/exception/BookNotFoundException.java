package com.library.catalog.domain.exception;

public class BookNotFoundException extends DomainException {
    public BookNotFoundException(String message) {
        super("BOOKNOTFOUND", message);
    }
}
