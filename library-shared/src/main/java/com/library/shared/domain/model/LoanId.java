package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class LoanId extends AggregateId {

    protected LoanId() {
    }

    private LoanId(String value) {
        super(value);
    }

    public static LoanId generate() {
        return new LoanId(generateUUID());
    }

    public static LoanId of(String value) {
        return new LoanId(value);
    }
}
