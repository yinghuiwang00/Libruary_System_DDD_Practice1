package com.library.analytics.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BookCreatedAnalyticsHandler {
    private static final Logger log = LoggerFactory.getLogger(BookCreatedAnalyticsHandler.class);

    public void handle(JsonNode event) {
        String bookId = event.get("bookId").asText();
        String title = event.has("title") ? event.get("title").asText() : "Unknown";
        log.info("Tracking new book acquisition: bookId={}, title={}", bookId, title);
    }
}
