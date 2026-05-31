package com.library.analytics.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.analytics.application.handler.LowStockAnalyticsHandler;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("embedded-kafka")
@EmbeddedKafka(partitions = 1, topics = {"library.inventory.events"})
@DirtiesContext
class InventoryEventConsumerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private KafkaTemplate<String, String> kafkaTemplate;

    @SpyBean
    private LowStockAnalyticsHandler lowStockHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(pf);
    }

    @Test
    void shouldHandleLowStockAlertEvent() {
        String eventJson = """
            {
                "eventType": "LowStockAlertEvent",
                "eventId": "evt-inv-001",
                "bookId": "book-789",
                "availableCopies": 2
            }
            """;

        kafkaTemplate.send("library.inventory.events", "evt-inv-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
            verify(lowStockHandler).handle(captor.capture());
            JsonNode captured = captor.getValue();
            assertThat(captured.get("bookId").asText()).isEqualTo("book-789");
            assertThat(captured.get("availableCopies").asInt()).isEqualTo(2);
        });
    }

    @Test
    void shouldIgnoreNonLowStockAlertEvent() {
        String eventJson = """
            {
                "eventType": "CopyAddedEvent",
                "eventId": "evt-inv-002",
                "bookId": "book-999"
            }
            """;

        kafkaTemplate.send("library.inventory.events", "evt-inv-002", eventJson);

        await().during(2, SECONDS).untilAsserted(() ->
            verify(lowStockHandler, never()).handle(any())
        );
    }
}
