package com.library.circulation.domain.exception;

public class HoldNotFoundException extends DomainException {
    public HoldNotFoundException(Object holdId) {
        super("HOLD_NOT_FOUND", "Hold not found: " + holdId);
    }
}
