package com.library.circulation.domain.exception;

public class LoanNotFoundException extends DomainException {
    public LoanNotFoundException(Object loanId) {
        super("LOAN_NOT_FOUND", "Loan not found: " + loanId);
    }
}
