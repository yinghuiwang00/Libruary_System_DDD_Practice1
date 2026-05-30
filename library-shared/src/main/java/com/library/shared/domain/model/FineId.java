package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class FineId extends AggregateId {

    protected FineId() {
    }

    private FineId(String value) {
        super(value);
    }

    public static FineId generate() {
        return new FineId(generateUUID());
    }

    public static FineId of(String value) {
        return new FineId(value);
    }
}
