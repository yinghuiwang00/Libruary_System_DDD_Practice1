package com.library.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.application.handler.BorrowedNotificationHandler;
import com.library.notification.application.handler.ReturnedNotificationHandler;
import com.library.notification.application.handler.OverdueNotificationHandler;
import com.library.notification.application.handler.FineNotificationHandler;
import com.library.notification.application.handler.HoldNotificationHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("notificationCirculationEventConsumer")
public class CirculationEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CirculationEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final BorrowedNotificationHandler borrowedHandler;
    private final ReturnedNotificationHandler returnedHandler;
    private final OverdueNotificationHandler overdueHandler;
    private final FineNotificationHandler fineHandler;
    private final HoldNotificationHandler holdHandler;

    public CirculationEventConsumer(ObjectMapper objectMapper, BorrowedNotificationHandler borrowedHandler,
            ReturnedNotificationHandler returnedHandler, OverdueNotificationHandler overdueHandler,
            FineNotificationHandler fineHandler, HoldNotificationHandler holdHandler) {
        this.objectMapper = objectMapper; this.borrowedHandler = borrowedHandler; this.returnedHandler = returnedHandler;
        this.overdueHandler = overdueHandler; this.fineHandler = fineHandler; this.holdHandler = holdHandler;
    }

    @KafkaListener(topics = "library.circulation.events", groupId = "library.notification.consumer.circulation")
    public void onCirculationEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received circulation event: {}", eventType);
            switch (eventType) {
                case "BookBorrowedEvent" -> borrowedHandler.handle(event);
                case "BookReturnedEvent" -> returnedHandler.handle(event);
                case "OverdueNoticeEvent" -> overdueHandler.handle(event);
                case "FineIncurredEvent" -> fineHandler.handle(event);
                case "HoldPlacedEvent" -> holdHandler.handleHoldPlaced(event);
                case "HoldFulfilledEvent" -> holdHandler.handleHoldFulfilled(event);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) { log.error("Error: {}", e.getMessage(), e); }
    }
}
