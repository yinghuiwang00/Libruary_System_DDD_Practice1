package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class BookId extends AggregateId {

    protected BookId() {
    }

    private BookId(String value) {
        super(value);
    }

    public static BookId generate() {
        return new BookId(generateUUID());
    }

    public static BookId of(String value) {
        return new BookId(value);
    }
}
