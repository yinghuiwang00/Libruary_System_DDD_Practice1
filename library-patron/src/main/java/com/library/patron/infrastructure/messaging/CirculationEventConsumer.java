package com.library.patron.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.patron.application.handler.BookBorrowedEventHandler;
import com.library.patron.application.handler.BookReturnedEventHandler;
import com.library.patron.application.handler.FineIncurredEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("patronCirculationEventConsumer")
public class CirculationEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CirculationEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final BookBorrowedEventHandler borrowedHandler;
    private final BookReturnedEventHandler returnedHandler;
    private final FineIncurredEventHandler fineHandler;

    public CirculationEventConsumer(ObjectMapper objectMapper, BookBorrowedEventHandler borrowedHandler, BookReturnedEventHandler returnedHandler, FineIncurredEventHandler fineHandler) {
        this.objectMapper = objectMapper; this.borrowedHandler = borrowedHandler; this.returnedHandler = returnedHandler; this.fineHandler = fineHandler;
    }

    @KafkaListener(topics = "library.circulation.events", groupId = "library.patron.consumer.circulation")
    public void onCirculationEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();
            log.info("Received circulation event: {}", eventType);
            switch (eventType) {
                case "BookBorrowedEvent" -> borrowedHandler.handle(event);
                case "BookReturnedEvent" -> returnedHandler.handle(event);
                case "FineIncurredEvent" -> fineHandler.handle(event);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) { log.error("Error processing circulation event: {}", e.getMessage(), e); }
    }
}
