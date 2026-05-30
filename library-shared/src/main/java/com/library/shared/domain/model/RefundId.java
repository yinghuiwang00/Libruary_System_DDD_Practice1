package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class RefundId extends AggregateId {

    protected RefundId() {
    }

    private RefundId(String value) {
        super(value);
    }

    public static RefundId generate() {
        return new RefundId(generateUUID());
    }

    public static RefundId of(String value) {
        return new RefundId(value);
    }
}
