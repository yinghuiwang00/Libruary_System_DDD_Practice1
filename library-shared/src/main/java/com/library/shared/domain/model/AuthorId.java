package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class AuthorId extends AggregateId {

    protected AuthorId() {
    }

    private AuthorId(String value) {
        super(value);
    }

    public static AuthorId generate() {
        return new AuthorId(generateUUID());
    }

    public static AuthorId of(String value) {
        return new AuthorId(value);
    }
}
