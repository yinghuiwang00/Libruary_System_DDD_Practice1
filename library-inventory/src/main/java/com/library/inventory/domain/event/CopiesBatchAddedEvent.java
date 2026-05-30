package com.library.inventory.domain.event;

import com.library.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public class CopiesBatchAddedEvent extends DomainEvent {
    private final String inventoryId;
    private final String bookId;
    private final int count;
    private final LocalDateTime addedAt;

    public CopiesBatchAddedEvent(String inventoryId, String bookId, int count) {
        super();
        this.inventoryId = inventoryId;
        this.bookId = bookId;
        this.count = count;
        this.addedAt = LocalDateTime.now();
    }

    public String getInventoryId() { return inventoryId; }
    public String getBookId() { return bookId; }
    public int getCount() { return count; }
    public LocalDateTime getAddedAt() { return addedAt; }
}
