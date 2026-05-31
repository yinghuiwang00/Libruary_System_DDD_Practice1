package com.library.inventory.integration;

import com.library.inventory.domain.model.*;
import com.library.inventory.domain.model.enums.CopyStatus;
import com.library.inventory.domain.repository.BookCopyRepository;
import com.library.inventory.domain.repository.CopyInventoryRepository;
import com.library.inventory.domain.repository.LibraryRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"library.circulation.events"})
@ActiveProfiles("embedded-kafka")
@DirtiesContext
class CirculationEventConsumerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private CopyInventoryRepository inventoryRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Clean up tables to avoid unique constraint violations across tests
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("DELETE FROM book_copies");
            jdbcTemplate.execute("DELETE FROM copy_inventories");
            jdbcTemplate.execute("DELETE FROM libraries");
        });

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
    }

    @Test
    void shouldMarkCopyAsBorrowedWhenBookBorrowedEventReceived() {
        // Seed library + inventory + copy with unique barcode
        String barcode = "BC-CIRC-" + UUID.randomUUID().toString().substring(0, 8);
        Library library = libraryRepository.save(Library.create("MAIN-LIB-001", "Main Library"));
        CopyInventory inventory = CopyInventory.create("BOOK-CIRC-001", library.getId().getValue(), library.getCode(), "SYSTEM");
        inventory.addCopy(barcode, Location.simple("MAIN", "DEFAULT"), "PURCHASE");
        CopyInventory savedInventory = inventoryRepository.save(inventory);
        BookCopy testCopy = savedInventory.getCopies().get(0);

        String eventJson = """
            {
                "eventType": "BookBorrowedEvent",
                "eventId": "evt-borrow-001",
                "copyId": {"value": "%s"},
                "bookId": {"value": "BOOK-CIRC-001"},
                "loanId": {"value": "LOAN-001"},
                "patronId": {"value": "PATRON-001"}
            }
            """.formatted(testCopy.getId().getValue());

        kafkaTemplate.send("library.circulation.events", "evt-borrow-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            BookCopy updated = copyRepository.findById(testCopy.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(CopyStatus.BORROWED);
        });

        CopyInventory updatedInventory = inventoryRepository.findById(savedInventory.getId()).orElseThrow();
        assertThat(updatedInventory.getAvailableCopies()).isEqualTo(0);
    }

    @Test
    void shouldMarkCopyAsAvailableWhenBookReturnedEventReceived() {
        // Seed library + inventory + copy with unique barcode
        String barcode = "BC-CIRC-" + UUID.randomUUID().toString().substring(0, 8);
        Library library = libraryRepository.save(Library.create("MAIN-LIB-002", "Second Library"));
        CopyInventory inventory = CopyInventory.create("BOOK-CIRC-002", library.getId().getValue(), library.getCode(), "SYSTEM");
        inventory.addCopy(barcode, Location.simple("MAIN", "DEFAULT"), "PURCHASE");
        CopyInventory savedInventory = inventoryRepository.save(inventory);

        // Manually mark copy as borrowed to set up precondition
        // Re-read from DB to get the correct @Version for optimistic locking
        BookCopy testCopy = copyRepository.findAll().get(0);
        testCopy.markAsBorrowed();
        CopyStatus oldStatus = testCopy.getStatus();
        savedInventory.onCopyStatusChanged(CopyStatus.AVAILABLE, oldStatus);
        inventoryRepository.save(savedInventory);
        BookCopy savedCopy = copyRepository.save(testCopy);
        final var copyId = savedCopy.getId();

        // Wait for any side-effect events (from Publisher) to be fully consumed
        // before sending the Return event, to avoid concurrent version conflicts
        await().atMost(3, SECONDS).pollDelay(2, SECONDS).untilAsserted(() ->
            assertThat(copyRepository.findById(copyId).orElseThrow().getVersion())
                .isNotNull()
        );

        String eventJson = """
            {
                "eventType": "BookReturnedEvent",
                "eventId": "evt-return-001",
                "copyId": {"value": "%s"},
                "bookId": {"value": "BOOK-CIRC-002"},
                "loanId": {"value": "LOAN-001"},
                "patronId": {"value": "PATRON-001"}
            }
            """.formatted(copyId.getValue());

        kafkaTemplate.send("library.circulation.events", "evt-return-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            BookCopy updated = copyRepository.findById(copyId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        });

        CopyInventory updatedInventory = inventoryRepository.findById(savedInventory.getId()).orElseThrow();
        assertThat(updatedInventory.getAvailableCopies()).isEqualTo(1);
    }
}
