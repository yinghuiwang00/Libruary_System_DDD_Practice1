package com.library.inventory.domain.event;

import com.library.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public class CopyAddedEvent extends DomainEvent {
    private final String copyId;
    private final String inventoryId;
    private final String bookId;
    private final String barcode;
    private final LocalDateTime addedAt;

    public CopyAddedEvent(String copyId, String inventoryId, String bookId, String barcode) {
        super();
        this.copyId = copyId;
        this.inventoryId = inventoryId;
        this.bookId = bookId;
        this.barcode = barcode;
        this.addedAt = LocalDateTime.now();
    }

    public String getCopyId() { return copyId; }
    public String getInventoryId() { return inventoryId; }
    public String getBookId() { return bookId; }
    public String getBarcode() { return barcode; }
    public LocalDateTime getAddedAt() { return addedAt; }
}
