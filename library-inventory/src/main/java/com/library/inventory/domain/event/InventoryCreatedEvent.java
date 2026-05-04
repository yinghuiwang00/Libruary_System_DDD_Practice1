package com.library.inventory.domain.event;

import com.library.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public class InventoryCreatedEvent extends DomainEvent {
    private final String inventoryId;
    private final String bookId;
    private final String libraryId;
    private final int initialCopyCount;
    private final LocalDateTime createdAt;

    public InventoryCreatedEvent(String inventoryId, String bookId, String libraryId, int initialCopyCount) {
        super();
        this.inventoryId = inventoryId;
        this.bookId = bookId;
        this.libraryId = libraryId;
        this.initialCopyCount = initialCopyCount;
        this.createdAt = LocalDateTime.now();
    }

    public String getInventoryId() { return inventoryId; }
    public String getBookId() { return bookId; }
    public String getLibraryId() { return libraryId; }
    public int getInitialCopyCount() { return initialCopyCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
