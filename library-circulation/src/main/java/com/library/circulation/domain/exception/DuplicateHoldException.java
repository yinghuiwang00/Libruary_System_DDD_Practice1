package com.library.circulation.domain.exception;

public class DuplicateHoldException extends DomainException {
    public DuplicateHoldException(Object bookId, Object patronId) {
        super("DUPLICATE_HOLD", "Patron " + patronId + " already has a hold on book " + bookId);
    }
}
