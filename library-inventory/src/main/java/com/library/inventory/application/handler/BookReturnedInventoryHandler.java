package com.library.inventory.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.inventory.domain.service.InventoryManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookReturnedInventoryHandler {
    private static final Logger log = LoggerFactory.getLogger(BookReturnedInventoryHandler.class);
    private final InventoryManagementService inventoryService;

    public BookReturnedInventoryHandler(InventoryManagementService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Transactional
    public void handle(JsonNode event) {
        String copyId = event.get("copyId").get("value").asText();
        log.info("Handling BookReturnedEvent for copyId: {}", copyId);
        try {
            inventoryService.returnCopy(copyId);
        } catch (Exception e) {
            log.error("Failed to return copy {}: {}", copyId, e.getMessage(), e);
        }
    }
}
