package com.library.analytics.infrastructure.messaging;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("analyticsDomainEventPublisher")
public class AnalyticsDomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsDomainEventPublisher.class);

    private final DomainEventPublisher localPublisher;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final String topicName;

    public AnalyticsDomainEventPublisher(
            DomainEventPublisher localPublisher,
            ObjectProvider<KafkaTemplate<String, DomainEvent>> kafkaTemplateProvider,
            org.springframework.core.env.Environment environment) {
        this.localPublisher = localPublisher;
        this.kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        this.topicName = environment.getProperty(
            "spring.kafka.topic.domain-events", "library.analytics.events");
    }

    public void publish(DomainEvent event) {
        localPublisher.publish(event);
        if (kafkaTemplate == null) {
            log.debug("KafkaTemplate not available, skipping Kafka publish for event {}", event.getEventType());
            return;
        }
        try {
            kafkaTemplate.send(topicName, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} to Kafka: {}", event.getEventType(), ex.getMessage(), ex);
                    } else {
                        log.debug("Published event {} to topic {} with key {}", event.getEventType(), topicName, event.getEventId());
                    }
                });
        } catch (Exception e) {
            log.error("Error sending event {} to Kafka: {}", event.getEventType(), e.getMessage(), e);
        }
    }

    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
