package com.library.inventory.domain.event;

import com.library.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public class LowStockAlertEvent extends DomainEvent {
    private final String inventoryId;
    private final String bookId;
    private final int availableCopies;
    private final int threshold;
    private final LocalDateTime alertedAt;

    public LowStockAlertEvent(String inventoryId, String bookId, int availableCopies, int threshold) {
        super();
        this.inventoryId = inventoryId;
        this.bookId = bookId;
        this.availableCopies = availableCopies;
        this.threshold = threshold;
        this.alertedAt = LocalDateTime.now();
    }

    public String getInventoryId() { return inventoryId; }
    public String getBookId() { return bookId; }
    public int getAvailableCopies() { return availableCopies; }
    public int getThreshold() { return threshold; }
    public LocalDateTime getAlertedAt() { return alertedAt; }
}
