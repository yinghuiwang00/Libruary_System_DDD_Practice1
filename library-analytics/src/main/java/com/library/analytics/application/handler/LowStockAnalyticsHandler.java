package com.library.analytics.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LowStockAnalyticsHandler {
    private static final Logger log = LoggerFactory.getLogger(LowStockAnalyticsHandler.class);

    public void handle(JsonNode event) {
        String bookId = event.get("bookId").asText();
        int copies = event.get("availableCopies").asInt();
        log.info("Tracking low stock alert: bookId={}, availableCopies={}", bookId, copies);
    }
}
