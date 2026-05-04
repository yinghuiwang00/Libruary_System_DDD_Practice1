package com.library.catalog.domain.exception;

public class CategoryNotFoundException extends DomainException {
    public CategoryNotFoundException(String message) {
        super("CATEGORYNOTFOUND", message);
    }
}
