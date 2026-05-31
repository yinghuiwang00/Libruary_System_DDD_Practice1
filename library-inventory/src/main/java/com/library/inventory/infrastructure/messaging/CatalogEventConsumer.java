package com.library.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.inventory.application.handler.BookCreatedEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("inventoryCatalogEventConsumer")
public class CatalogEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CatalogEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final BookCreatedEventHandler bookCreatedHandler;

    public CatalogEventConsumer(ObjectMapper objectMapper, BookCreatedEventHandler bookCreatedHandler) {
        this.objectMapper = objectMapper;
        this.bookCreatedHandler = bookCreatedHandler;
    }

    @KafkaListener(topics = "library.catalog.events", groupId = "library.inventory.consumer.catalog")
    public void onCatalogEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received catalog event: {}", eventType);
            switch (eventType) {
                case "BookCreatedEvent" -> bookCreatedHandler.handle(event);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing catalog event: {}", e.getMessage(), e);
        }
    }
}
