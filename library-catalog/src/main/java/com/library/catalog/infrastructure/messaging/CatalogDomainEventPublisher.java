package com.library.catalog.infrastructure.messaging;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Catalog-specific domain event publisher that publishes events
 * both locally via Spring ApplicationEventPublisher and remotely via Kafka.
 */
@Component("catalogDomainEventPublisher")
public class CatalogDomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CatalogDomainEventPublisher.class);

    private final DomainEventPublisher localPublisher;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final String topicName;

    public CatalogDomainEventPublisher(
            DomainEventPublisher localPublisher,
            KafkaTemplate<String, DomainEvent> kafkaTemplate,
            org.springframework.core.env.Environment environment) {
        this.localPublisher = localPublisher;
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = environment.getProperty(
            "spring.kafka.topic.domain-events", "library.domain-events");
    }

    public void publish(DomainEvent event) {
        // Publish locally for in-process event handling
        localPublisher.publish(event);

        // Publish to Kafka for cross-service communication
        try {
            kafkaTemplate.send(topicName, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} to Kafka: {}",
                            event.getEventType(), ex.getMessage(), ex);
                    } else {
                        log.debug("Published event {} to topic {} with key {}",
                            event.getEventType(), topicName, event.getEventId());
                    }
                });
        } catch (Exception e) {
            log.error("Error sending event {} to Kafka: {}",
                event.getEventType(), e.getMessage(), e);
        }
    }

    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
