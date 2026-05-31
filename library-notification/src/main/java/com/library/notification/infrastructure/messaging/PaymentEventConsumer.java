package com.library.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.application.handler.PaymentNotificationHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("notificationPaymentEventConsumer")
public class PaymentEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final PaymentNotificationHandler paymentHandler;

    public PaymentEventConsumer(ObjectMapper objectMapper, PaymentNotificationHandler paymentHandler) {
        this.objectMapper = objectMapper; this.paymentHandler = paymentHandler;
    }

    @KafkaListener(topics = "library.payment.events", groupId = "library.notification.consumer.payment")
    public void onPaymentEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received payment event: {}", eventType);
            if ("PaymentCompletedEvent".equals(eventType)) paymentHandler.handle(event);
        } catch (Exception e) { log.error("Error: {}", e.getMessage(), e); }
    }
}
