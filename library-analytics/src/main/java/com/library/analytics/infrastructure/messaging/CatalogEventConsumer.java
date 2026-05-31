package com.library.analytics.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.analytics.application.handler.BookCreatedAnalyticsHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("analyticsCatalogEventConsumer")
public class CatalogEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CatalogEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final BookCreatedAnalyticsHandler bookCreatedHandler;

    public CatalogEventConsumer(ObjectMapper objectMapper, BookCreatedAnalyticsHandler bookCreatedHandler) {
        this.objectMapper = objectMapper; this.bookCreatedHandler = bookCreatedHandler;
    }

    @KafkaListener(topics = "library.catalog.events", groupId = "library.analytics.consumer.catalog")
    public void onCatalogEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received catalog event: {}", eventType);
            if ("BookCreatedEvent".equals(eventType)) bookCreatedHandler.handle(event);
        } catch (Exception e) { log.error("Error: {}", e.getMessage(), e); }
    }
}
