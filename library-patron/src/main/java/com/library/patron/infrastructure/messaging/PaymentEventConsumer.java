package com.library.patron.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.patron.application.handler.PaymentCompletedEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("patronPaymentEventConsumer")
public class PaymentEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final PaymentCompletedEventHandler paymentHandler;

    public PaymentEventConsumer(ObjectMapper objectMapper, PaymentCompletedEventHandler paymentHandler) {
        this.objectMapper = objectMapper; this.paymentHandler = paymentHandler;
    }

    @KafkaListener(topics = "library.payment.events", groupId = "library.patron.consumer.payment")
    public void onPaymentEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received payment event: {}", eventType);
            if ("PaymentCompletedEvent".equals(eventType)) paymentHandler.handle(event);
        } catch (Exception e) { log.error("Error processing payment event: {}", e.getMessage(), e); }
    }
}
