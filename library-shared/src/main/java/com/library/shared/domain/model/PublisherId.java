package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class PublisherId extends AggregateId {

    protected PublisherId() {
    }

    private PublisherId(String value) {
        super(value);
    }

    public static PublisherId generate() {
        return new PublisherId(generateUUID());
    }

    public static PublisherId of(String value) {
        return new PublisherId(value);
    }
}
