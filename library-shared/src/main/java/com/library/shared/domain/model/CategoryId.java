package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class CategoryId extends AggregateId {

    protected CategoryId() {
    }

    private CategoryId(String value) {
        super(value);
    }

    public static CategoryId generate() {
        return new CategoryId(generateUUID());
    }

    public static CategoryId of(String value) {
        return new CategoryId(value);
    }
}
