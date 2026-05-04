package com.library.catalog.domain.exception;

public class BookNotFoundException extends DomainException {
    public BookNotFoundException(String message) {
        super("BOOK_NOT_FOUND", message);
    }
}
