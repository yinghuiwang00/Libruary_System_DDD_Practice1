package com.library.shared.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class LibraryId extends AggregateId {

    protected LibraryId() {
    }

    private LibraryId(String value) {
        super(value);
    }

    public static LibraryId generate() {
        return new LibraryId(generateUUID());
    }

    public static LibraryId of(String value) {
        return new LibraryId(value);
    }
}
