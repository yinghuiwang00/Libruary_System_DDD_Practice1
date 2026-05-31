package com.library.inventory.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.inventory.domain.service.InventoryManagementService;
import com.library.inventory.domain.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookCreatedEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BookCreatedEventHandler.class);
    private static final String DEFAULT_LIBRARY_ID = "MAIN-LIB-001";
    private final InventoryManagementService inventoryService;

    public BookCreatedEventHandler(InventoryManagementService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Transactional
    public void handle(JsonNode event) {
        String bookId = event.get("bookId").asText();
        log.info("Handling BookCreatedEvent for bookId: {}", bookId);
        try {
            // DEFAULT_LIBRARY_ID is a library code; resolve to actual ID
            var libraryOpt = inventoryService.findLibraryByCode(DEFAULT_LIBRARY_ID);
            if (libraryOpt.isEmpty()) {
                log.warn("Default library {} not found, skipping initial inventory creation", DEFAULT_LIBRARY_ID);
                return;
            }
            String libraryId = libraryOpt.get().getId().getValue();
            inventoryService.createInitialInventory(bookId, libraryId, 0, Location.simple("MAIN", "DEFAULT"), "SYSTEM");
        } catch (Exception e) {
            log.error("Failed to create initial inventory for book {}: {}", bookId, e.getMessage(), e);
        }
    }
}
