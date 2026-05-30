package com.library.patron.domain.exception;

import com.library.patron.domain.model.enums.MembershipStatus;

import java.math.BigDecimal;

public class PatronCannotBorrowException extends DomainException {

    public PatronCannotBorrowException(MembershipStatus status, int currentLoans, BigDecimal outstandingFines) {
        super("PATRON_CANNOT_BORROW",
            String.format("Patron cannot borrow: status=%s, currentLoans=%d, outstandingFines=%s",
                status, currentLoans, outstandingFines));
    }
}
