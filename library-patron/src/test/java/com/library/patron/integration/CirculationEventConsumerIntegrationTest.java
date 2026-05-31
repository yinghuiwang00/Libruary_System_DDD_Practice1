package com.library.patron.integration;

import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import com.library.shared.domain.model.PatronId;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
    private PatronRepository patronRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Clean up patrons table
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("DELETE FROM patrons");
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
    void shouldIncreasePatronLoanCountWhenBookBorrowedEventReceived() {
        // Prepare patron with 1 existing loan
        Patron patron = Patron.create("Borrow", "Test", "borrow-int@test.com", PatronType.STUDENT);
        patron.recordLoan();
        Patron saved = patronRepository.save(patron);
        final PatronId patronId = saved.getId();

        String eventJson = """
            {
                "eventType": "BookBorrowedEvent",
                "eventId": "evt-borrow-int-001",
                "patronId": {"value": "%s"},
                "copyId": {"value": "copy-1"},
                "bookId": {"value": "book-1"},
                "loanId": {"value": "loan-1"}
            }
            """.formatted(patronId.getValue());

        kafkaTemplate.send("library.circulation.events", "evt-borrow-int-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(patronId).orElseThrow();
            assertThat(updated.getCurrentLoans()).isEqualTo(2);
        });
    }

    @Test
    void shouldDecreasePatronLoanCountWhenBookReturnedEventReceived() {
        // Prepare patron with 2 existing loans
        Patron patron = Patron.create("Return", "Test", "return-int@test.com", PatronType.STUDENT);
        patron.recordLoan();
        patron.recordLoan();
        Patron saved = patronRepository.save(patron);
        final PatronId patronId = saved.getId();

        String eventJson = """
            {
                "eventType": "BookReturnedEvent",
                "eventId": "evt-return-int-001",
                "patronId": {"value": "%s"},
                "copyId": {"value": "copy-1"},
                "bookId": {"value": "book-1"},
                "loanId": {"value": "loan-1"}
            }
            """.formatted(patronId.getValue());

        kafkaTemplate.send("library.circulation.events", "evt-return-int-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(patronId).orElseThrow();
            assertThat(updated.getCurrentLoans()).isEqualTo(1);
        });
    }

    @Test
    void shouldIncreasePatronFineBalanceWhenFineIncurredEventReceived() {
        // Prepare patron with 0 fines
        Patron patron = Patron.create("Fine", "Test", "fine-int@test.com", PatronType.STUDENT);
        Patron saved = patronRepository.save(patron);
        final PatronId patronId = saved.getId();

        String eventJson = """
            {
                "eventType": "FineIncurredEvent",
                "eventId": "evt-fine-int-001",
                "patronId": {"value": "%s"},
                "fineId": {"value": "fine-1"},
                "loanId": {"value": "loan-1"},
                "amount": "25.00",
                "overdueDays": 5
            }
            """.formatted(patronId.getValue());

        kafkaTemplate.send("library.circulation.events", "evt-fine-int-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(patronId).orElseThrow();
            assertThat(updated.getOutstandingFines()).isEqualByComparingTo(BigDecimal.valueOf(25));
        });
    }

    @Test
    void shouldIgnoreUnknownEventType() {
        Patron patron = Patron.create("Unknown", "Test", "unknown-int@test.com", PatronType.STUDENT);
        Patron saved = patronRepository.save(patron);
        final PatronId patronId = saved.getId();

        String eventJson = """
            {
                "eventType": "UnknownEvent",
                "eventId": "evt-unknown-int-001",
                "patronId": {"value": "%s"}
            }
            """.formatted(patronId.getValue());

        kafkaTemplate.send("library.circulation.events", "evt-unknown-int-001", eventJson);

        // Wait a short period, then verify patron was NOT modified
        await().atMost(3, SECONDS).pollDelay(2, SECONDS).untilAsserted(() -> {
            Patron unchanged = patronRepository.findById(patronId).orElseThrow();
            assertThat(unchanged.getCurrentLoans()).isEqualTo(0);
            assertThat(unchanged.getOutstandingFines()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }
}
