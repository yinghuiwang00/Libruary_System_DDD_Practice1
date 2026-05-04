package com.library.inventory.domain.exception;

import com.library.shared.domain.model.BookId;

public class NoAvailableCopyException extends DomainException {
    public NoAvailableCopyException(BookId bookId) {
        super("INV-005", "No available copy for book: " + bookId);
    }
}
