package com.library.circulation.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.circulation.application.handler.PatronSuspendedEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("circulationPatronEventConsumer")
public class PatronEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PatronEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final PatronSuspendedEventHandler suspendedHandler;

    public PatronEventConsumer(ObjectMapper objectMapper, PatronSuspendedEventHandler suspendedHandler) {
        this.objectMapper = objectMapper; this.suspendedHandler = suspendedHandler;
    }

    @KafkaListener(topics = "library.patron.events", groupId = "library.circulation.consumer.patron")
    public void onPatronEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received patron event: {}", eventType);
            if ("PatronSuspendedEvent".equals(eventType)) suspendedHandler.handle(event);
        } catch (Exception e) { log.error("Error processing patron event: {}", e.getMessage(), e); }
    }
}
