package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class HoldId extends AggregateId {

    protected HoldId() {
    }

    private HoldId(String value) {
        super(value);
    }

    public static HoldId generate() {
        return new HoldId(generateUUID());
    }

    public static HoldId of(String value) {
        return new HoldId(value);
    }
}
