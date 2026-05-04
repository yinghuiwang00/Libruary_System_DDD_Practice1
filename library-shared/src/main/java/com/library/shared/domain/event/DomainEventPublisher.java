package com.library.shared.domain.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public DomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }

    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
