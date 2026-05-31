package com.library.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.inventory.application.handler.BookBorrowedInventoryHandler;
import com.library.inventory.application.handler.BookReturnedInventoryHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("inventoryCirculationEventConsumer")
public class CirculationEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CirculationEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final BookBorrowedInventoryHandler borrowedHandler;
    private final BookReturnedInventoryHandler returnedHandler;

    public CirculationEventConsumer(ObjectMapper objectMapper, BookBorrowedInventoryHandler borrowedHandler, BookReturnedInventoryHandler returnedHandler) {
        this.objectMapper = objectMapper;
        this.borrowedHandler = borrowedHandler;
        this.returnedHandler = returnedHandler;
    }

    @KafkaListener(topics = "library.circulation.events", groupId = "library.inventory.consumer.circulation")
    public void onCirculationEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received circulation event: {}", eventType);
            switch (eventType) {
                case "BookBorrowedEvent" -> borrowedHandler.handle(event);
                case "BookReturnedEvent" -> returnedHandler.handle(event);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing circulation event: {}", e.getMessage(), e);
        }
    }
}
