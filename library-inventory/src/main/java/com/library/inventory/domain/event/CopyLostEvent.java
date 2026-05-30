package com.library.inventory.domain.event;

import com.library.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public class CopyLostEvent extends DomainEvent {
    private final String copyId;
    private final String inventoryId;
    private final String lostReason;
    private final LocalDateTime lostAt;

    public CopyLostEvent(String copyId, String inventoryId, String lostReason) {
        super();
        this.copyId = copyId;
        this.inventoryId = inventoryId;
        this.lostReason = lostReason;
        this.lostAt = LocalDateTime.now();
    }

    public String getCopyId() { return copyId; }
    public String getInventoryId() { return inventoryId; }
    public String getLostReason() { return lostReason; }
    public LocalDateTime getLostAt() { return lostAt; }
}
