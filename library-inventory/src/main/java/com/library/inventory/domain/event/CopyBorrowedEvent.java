package com.library.inventory.domain.event;

import com.library.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public class CopyBorrowedEvent extends DomainEvent {
    private final String copyId;
    private final String inventoryId;
    private final String bookId;
    private final String libraryId;
    private final LocalDateTime borrowedAt;

    public CopyBorrowedEvent(String copyId, String inventoryId, String bookId, String libraryId) {
        super();
        this.copyId = copyId;
        this.inventoryId = inventoryId;
        this.bookId = bookId;
        this.libraryId = libraryId;
        this.borrowedAt = LocalDateTime.now();
    }

    public String getCopyId() { return copyId; }
    public String getInventoryId() { return inventoryId; }
    public String getBookId() { return bookId; }
    public String getLibraryId() { return libraryId; }
    public LocalDateTime getBorrowedAt() { return borrowedAt; }
}
