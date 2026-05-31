package com.library.payment.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.payment.application.handler.FineIncurredEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("paymentCirculationEventConsumer")
public class CirculationEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CirculationEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final FineIncurredEventHandler fineHandler;

    public CirculationEventConsumer(ObjectMapper objectMapper, FineIncurredEventHandler fineHandler) {
        this.objectMapper = objectMapper; this.fineHandler = fineHandler;
    }

    @KafkaListener(topics = "library.circulation.events", groupId = "library.payment.consumer.circulation")
    public void onCirculationEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received circulation event: {}", eventType);
            if ("FineIncurredEvent".equals(eventType)) fineHandler.handle(event);
        } catch (Exception e) { log.error("Error processing circulation event: {}", e.getMessage(), e); }
    }
}
