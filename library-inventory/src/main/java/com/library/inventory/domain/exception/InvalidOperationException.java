package com.library.inventory.domain.exception;

public class InvalidOperationException extends DomainException {
    public InvalidOperationException(String message) {
        super("INV-006", message);
    }
}
