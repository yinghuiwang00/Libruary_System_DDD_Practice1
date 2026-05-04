package com.library.inventory.domain.exception;

import com.library.shared.domain.model.LibraryId;

public class LibraryNotFoundException extends DomainException {
    public LibraryNotFoundException(LibraryId id) {
        super("INV-003", "Library not found: " + id);
    }
}
