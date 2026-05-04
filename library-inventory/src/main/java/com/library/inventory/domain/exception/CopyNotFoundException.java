package com.library.inventory.domain.exception;

import com.library.shared.domain.model.CopyId;

public class CopyNotFoundException extends DomainException {
    public CopyNotFoundException(CopyId id) {
        super("INV-002", "Copy not found: " + id);
    }
}
