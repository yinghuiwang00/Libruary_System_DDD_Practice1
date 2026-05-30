package com.library.circulation.domain.exception;

public class LoanRenewalException extends DomainException {
    public LoanRenewalException(String message) {
        super("LOAN_RENEWAL_FAILED", message);
    }
}
