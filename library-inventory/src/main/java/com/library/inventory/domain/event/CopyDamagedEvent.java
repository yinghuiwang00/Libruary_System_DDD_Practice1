package com.library.inventory.domain.event;

import com.library.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public class CopyDamagedEvent extends DomainEvent {
    private final String copyId;
    private final String inventoryId;
    private final String damageDescription;
    private final LocalDateTime damagedAt;

    public CopyDamagedEvent(String copyId, String inventoryId, String damageDescription) {
        super();
        this.copyId = copyId;
        this.inventoryId = inventoryId;
        this.damageDescription = damageDescription;
        this.damagedAt = LocalDateTime.now();
    }

    public String getCopyId() { return copyId; }
    public String getInventoryId() { return inventoryId; }
    public String getDamageDescription() { return damageDescription; }
    public LocalDateTime getDamagedAt() { return damagedAt; }
}
