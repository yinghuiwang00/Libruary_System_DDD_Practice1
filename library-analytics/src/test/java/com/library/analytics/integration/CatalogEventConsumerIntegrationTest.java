package com.library.analytics.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.analytics.application.handler.BookCreatedAnalyticsHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("embedded-kafka")
@EmbeddedKafka(partitions = 1, topics = {"library.catalog.events"})
@DirtiesContext
class CatalogEventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @SpyBean
    private BookCreatedAnalyticsHandler bookCreatedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldHandleBookCreatedEvent() {
        String eventJson = """
            {
                "eventType": "BookCreatedEvent",
                "eventId": "evt-book-001",
                "bookId": "book-123",
                "title": "Domain-Driven Design"
            }
            """;

        kafkaTemplate.send("library.catalog.events", "evt-book-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
            verify(bookCreatedHandler).handle(captor.capture());
            JsonNode captured = captor.getValue();
            assertThat(captured.get("bookId").asText()).isEqualTo("book-123");
            assertThat(captured.get("title").asText()).isEqualTo("Domain-Driven Design");
        });
    }

    @Test
    void shouldIgnoreNonBookCreatedEvent() {
        String eventJson = """
            {
                "eventType": "BookUpdatedEvent",
                "eventId": "evt-book-002",
                "bookId": "book-456"
            }
            """;

        kafkaTemplate.send("library.catalog.events", "evt-book-002", eventJson);

        await().during(2, SECONDS).untilAsserted(() ->
            verify(bookCreatedHandler, never()).handle(any())
        );
    }
}
