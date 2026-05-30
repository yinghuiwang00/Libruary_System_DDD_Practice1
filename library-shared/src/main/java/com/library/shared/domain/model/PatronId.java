package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class PatronId extends AggregateId {

    protected PatronId() {
    }

    private PatronId(String value) {
        super(value);
    }

    public static PatronId generate() {
        return new PatronId(generateUUID());
    }

    public static PatronId of(String value) {
        return new PatronId(value);
    }
}
