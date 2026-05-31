package com.library.inventory.integration;

import com.library.inventory.domain.model.CopyInventory;
import com.library.inventory.domain.model.Library;
import com.library.inventory.domain.repository.CopyInventoryRepository;
import com.library.inventory.domain.repository.LibraryRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"library.catalog.events"})
@ActiveProfiles("embedded-kafka")
@DirtiesContext
class CatalogEventConsumerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private CopyInventoryRepository inventoryRepository;

    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Wait for all Kafka listener containers to be ready
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
        }

        // Create producer for sending raw JSON strings
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(pf);

        // Seed the library required by BookCreatedEventHandler (DEFAULT_LIBRARY_ID = "MAIN-LIB-001")
        libraryRepository.findByCode("MAIN-LIB-001").orElseGet(() -> {
            Library library = Library.create("MAIN-LIB-001", "Main Library");
            return libraryRepository.save(library);
        });
    }

    @Test
    void shouldCreateInventoryWhenBookCreatedEventReceived() {
        String bookId = "BOOK-NEW-001";

        String eventJson = """
            {
                "eventType": "BookCreatedEvent",
                "eventId": "evt-book-created-001",
                "bookId": "%s"
            }
            """.formatted(bookId);

        kafkaTemplate.send("library.catalog.events", "evt-book-created-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            var inventories = inventoryRepository.findByBookId(bookId);
            assertThat(inventories).hasSize(1);
            CopyInventory inventory = inventories.get(0);
            assertThat(inventory.getBookId()).isEqualTo(bookId);
            assertThat(inventory.getTotalCopies()).isEqualTo(0);
            assertThat(inventory.getAvailableCopies()).isEqualTo(0);
        });
    }

    @Test
    void shouldIgnoreUnknownEventType() {
        String bookId = "BOOK-IGNORE-001";

        String eventJson = """
            {
                "eventType": "UnknownEvent",
                "eventId": "evt-unknown-001",
                "bookId": "%s"
            }
            """.formatted(bookId);

        kafkaTemplate.send("library.catalog.events", "evt-unknown-001", eventJson);

        // Wait a short period, then verify no inventory was created
        await().atMost(3, SECONDS).pollDelay(2, SECONDS).untilAsserted(() -> {
            var inventories = inventoryRepository.findByBookId(bookId);
            assertThat(inventories).isEmpty();
        });
    }
}
