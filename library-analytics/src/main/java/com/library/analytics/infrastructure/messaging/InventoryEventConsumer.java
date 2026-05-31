package com.library.analytics.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.analytics.application.handler.LowStockAnalyticsHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("analyticsInventoryEventConsumer")
public class InventoryEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final LowStockAnalyticsHandler lowStockHandler;

    public InventoryEventConsumer(ObjectMapper objectMapper, LowStockAnalyticsHandler lowStockHandler) {
        this.objectMapper = objectMapper; this.lowStockHandler = lowStockHandler;
    }

    @KafkaListener(topics = "library.inventory.events", groupId = "library.analytics.consumer.inventory")
    public void onInventoryEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received inventory event: {}", eventType);
            if ("LowStockAlertEvent".equals(eventType)) lowStockHandler.handle(event);
        } catch (Exception e) { log.error("Error: {}", e.getMessage(), e); }
    }
}
