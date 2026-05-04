package com.library.catalog.domain.exception;

public class AuthorNotFoundException extends DomainException {

    public AuthorNotFoundException(String message) {
        super("AUTHOR_NOT_FOUND", message);
    }
}
