package com.library.circulation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.circulation.application.handler.PatronSuspendedEventHandler;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"library.patron.events"})
@ActiveProfiles("embedded-kafka")
@DirtiesContext
class PatronEventConsumerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @SpyBean
    private PatronSuspendedEventHandler suspendedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Create String-based producer manually to avoid type conflict with DomainEvent KafkaTemplate
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(pf);
    }

    @Test
    void shouldHandlePatronSuspendedEvent() {
        String eventJson = """
            {
                "eventType": "PatronSuspendedEvent",
                "eventId": "evt-patron-suspended-001",
                "patronId": {"value": "PATRON-001"},
                "reason": "Overdue fines exceeded limit"
            }
            """;

        kafkaTemplate.send("library.patron.events", "evt-patron-suspended-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            ArgumentCaptor<com.fasterxml.jackson.databind.JsonNode> captor =
                ArgumentCaptor.forClass(com.fasterxml.jackson.databind.JsonNode.class);
            org.mockito.Mockito.verify(suspendedHandler).handle(captor.capture());
            com.fasterxml.jackson.databind.JsonNode captured = captor.getValue();
            assertThat(captured.get("patronId").get("value").asText()).isEqualTo("PATRON-001");
            assertThat(captured.get("reason").asText()).isEqualTo("Overdue fines exceeded limit");
        });
    }

    @Test
    void shouldIgnoreNonPatronSuspendedEvent() {
        String eventJson = """
            {
                "eventType": "PatronActivatedEvent",
                "eventId": "evt-patron-activated-001",
                "patronId": {"value": "PATRON-002"}
            }
            """;

        kafkaTemplate.send("library.patron.events", "evt-patron-activated-001", eventJson);

        await().during(2, SECONDS).untilAsserted(() ->
            org.mockito.Mockito.verify(suspendedHandler, org.mockito.Mockito.never())
                .handle(org.mockito.ArgumentMatchers.any())
        );
    }
}
