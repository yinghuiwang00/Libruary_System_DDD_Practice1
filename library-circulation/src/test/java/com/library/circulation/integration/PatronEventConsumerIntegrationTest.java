package com.library.circulation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.circulation.application.handler.PatronSuspendedEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"library.patron.events"})
@ActiveProfiles("embedded-kafka")
@DirtiesContext
class PatronEventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @SpyBean
    private PatronSuspendedEventHandler suspendedHandler;

    @Autowired
    private ObjectMapper objectMapper;

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
