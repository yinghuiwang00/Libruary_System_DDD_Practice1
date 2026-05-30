package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class PaymentId extends AggregateId {

    protected PaymentId() {
    }

    private PaymentId(String value) {
        super(value);
    }

    public static PaymentId generate() {
        return new PaymentId(generateUUID());
    }

    public static PaymentId of(String value) {
        return new PaymentId(value);
    }
}
