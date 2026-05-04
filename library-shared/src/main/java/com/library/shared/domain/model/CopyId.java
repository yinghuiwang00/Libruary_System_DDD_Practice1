package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class CopyId extends AggregateId {

    protected CopyId() {
    }

    private CopyId(String value) {
        super(value);
    }

    public static CopyId generate() {
        return new CopyId(generateUUID());
    }

    public static CopyId of(String value) {
        return new CopyId(value);
    }
}
