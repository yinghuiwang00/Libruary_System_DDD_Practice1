package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class CopyInventoryId extends AggregateId {

    protected CopyInventoryId() {
    }

    private CopyInventoryId(String value) {
        super(value);
    }

    public static CopyInventoryId generate() {
        return new CopyInventoryId(generateUUID());
    }

    public static CopyInventoryId of(String value) {
        return new CopyInventoryId(value);
    }
}
