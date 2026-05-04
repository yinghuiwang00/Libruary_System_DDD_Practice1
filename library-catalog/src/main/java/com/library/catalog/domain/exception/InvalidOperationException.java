package com.library.catalog.domain.exception;

public class InvalidOperationException extends DomainException {
    public InvalidOperationException(String message) {
        super("INVALIDOPERATION", message);
    }
}
