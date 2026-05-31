package com.library.circulation.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PatronSuspendedEventHandler {
    private static final Logger log = LoggerFactory.getLogger(PatronSuspendedEventHandler.class);

    public void handle(JsonNode event) {
        String patronId = event.get("patronId").get("value").asText();
        String reason = event.has("reason") ? event.get("reason").asText() : "Unknown";
        log.info("Patron {} suspended, reason: {}. Future borrowings will be blocked.", patronId, reason);
    }
}
