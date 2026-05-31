package com.library.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.application.handler.PatronStatusNotificationHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("notificationPatronEventConsumer")
public class PatronEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PatronEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final PatronStatusNotificationHandler patronStatusHandler;

    public PatronEventConsumer(ObjectMapper objectMapper, PatronStatusNotificationHandler patronStatusHandler) {
        this.objectMapper = objectMapper; this.patronStatusHandler = patronStatusHandler;
    }

    @KafkaListener(topics = "library.patron.events", groupId = "library.notification.consumer.patron")
    public void onPatronEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received patron event: {}", eventType);
            if ("PatronSuspendedEvent".equals(eventType)) patronStatusHandler.handlePatronSuspended(event);
        } catch (Exception e) { log.error("Error: {}", e.getMessage(), e); }
    }
}
