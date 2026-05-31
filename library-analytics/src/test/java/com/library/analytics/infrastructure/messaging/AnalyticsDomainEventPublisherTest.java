package com.library.analytics.infrastructure.messaging;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.concurrent.CompletableFuture;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsDomainEventPublisherTest {
    @Mock private DomainEventPublisher localPublisher;
    @Mock private ObjectProvider<KafkaTemplate<String, DomainEvent>> kafkaProvider;
    @Mock private KafkaTemplate<String, DomainEvent> kafkaTemplate;
    @Mock private Environment environment;
    private AnalyticsDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        when(environment.getProperty("spring.kafka.topic.domain-events", "library.analytics.events")).thenReturn("library.analytics.events");
    }

    @Test
    void shouldPublishBothLocallyAndKafka() {
        when(kafkaProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        when(kafkaTemplate.send(anyString(), anyString(), any(DomainEvent.class))).thenReturn(new CompletableFuture<>());
        publisher = new AnalyticsDomainEventPublisher(localPublisher, kafkaProvider, environment);
        DomainEvent event = mock(DomainEvent.class);
        when(event.getEventId()).thenReturn("test-id");
        when(event.getEventType()).thenReturn("TestEvent");
        publisher.publish(event);
        verify(localPublisher).publish(event);
        verify(kafkaTemplate).send("library.analytics.events", "test-id", event);
    }

    @Test
    void shouldPublishOnlyLocallyWhenKafkaUnavailable() {
        when(kafkaProvider.getIfAvailable()).thenReturn(null);
        publisher = new AnalyticsDomainEventPublisher(localPublisher, kafkaProvider, environment);
        DomainEvent event = mock(DomainEvent.class);
        when(event.getEventType()).thenReturn("TestEvent");
        publisher.publish(event);
        verify(localPublisher).publish(event);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldNotFailWhenKafkaSendThrows() {
        when(kafkaProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        when(kafkaTemplate.send(anyString(), anyString(), any(DomainEvent.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka down")));
        publisher = new AnalyticsDomainEventPublisher(localPublisher, kafkaProvider, environment);
        DomainEvent event = mock(DomainEvent.class);
        when(event.getEventId()).thenReturn("test-id");
        when(event.getEventType()).thenReturn("TestEvent");
        publisher.publish(event);
        verify(localPublisher).publish(event);
    }
}
