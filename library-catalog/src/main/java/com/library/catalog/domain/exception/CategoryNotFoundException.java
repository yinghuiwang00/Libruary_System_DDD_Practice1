package com.library.catalog.domain.exception;

public class CategoryNotFoundException extends DomainException {
    public CategoryNotFoundException(String message) {
        super("CATEGORY_NOT_FOUND", message);
    }
}
