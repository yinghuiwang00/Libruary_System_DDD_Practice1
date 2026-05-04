package com.library.inventory.domain.exception;

import com.library.shared.domain.model.BookId;
import com.library.shared.domain.model.LibraryId;

public class DuplicateInventoryException extends DomainException {
    public DuplicateInventoryException(BookId bookId, LibraryId libraryId) {
        super("INV-004", "Inventory already exists for book " + bookId + " in library " + libraryId);
    }
}
