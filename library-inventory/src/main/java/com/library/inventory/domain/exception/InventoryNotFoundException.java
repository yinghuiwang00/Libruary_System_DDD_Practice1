package com.library.inventory.domain.exception;

import com.library.shared.domain.model.CopyInventoryId;

public class InventoryNotFoundException extends DomainException {
    public InventoryNotFoundException(CopyInventoryId id) {
        super("INV-001", "Inventory not found: " + id);
    }
}
