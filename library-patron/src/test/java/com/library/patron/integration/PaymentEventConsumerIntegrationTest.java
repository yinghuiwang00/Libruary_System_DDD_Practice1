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
@EmbeddedKafka(partitions = 1, topics = {"library.payment.events"})
@ActiveProfiles("embedded-kafka")
@DirtiesContext
class PaymentEventConsumerIntegrationTest {

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
    void shouldDecreasePatronFineBalanceWhenPaymentCompletedEventReceived() {
        // Prepare patron with outstanding fines
        Patron patron = Patron.create("Payment", "Test", "payment-int@test.com", PatronType.STUDENT);
        patron.addFine(BigDecimal.valueOf(50));
        Patron saved = patronRepository.save(patron);
        final PatronId patronId = saved.getId();

        String eventJson = """
            {
                "eventType": "PaymentCompletedEvent",
                "eventId": "evt-pay-int-001",
                "patronId": {"value": "%s"},
                "paymentId": {"value": "pay-1"},
                "amount": "25.00"
            }
            """.formatted(patronId.getValue());

        kafkaTemplate.send("library.payment.events", "evt-pay-int-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(patronId).orElseThrow();
            assertThat(updated.getOutstandingFines()).isEqualByComparingTo(BigDecimal.valueOf(25));
        });
    }

    @Test
    void shouldReactivatePatronWhenPaymentBringsFinesBelowThreshold() {
        // Prepare patron suspended due to excessive fines (>= 50)
        Patron patron = Patron.create("Reactivate", "Test", "reactivate-int@test.com", PatronType.STUDENT);
        patron.addFine(BigDecimal.valueOf(60));
        Patron saved = patronRepository.save(patron);
        final PatronId patronId = saved.getId();
        assertThat(saved.getStatus().name()).isEqualTo("SUSPENDED");

        // Pay enough to bring fines below threshold
        String eventJson = """
            {
                "eventType": "PaymentCompletedEvent",
                "eventId": "evt-pay-int-002",
                "patronId": {"value": "%s"},
                "paymentId": {"value": "pay-2"},
                "amount": "30.00"
            }
            """.formatted(patronId.getValue());

        kafkaTemplate.send("library.payment.events", "evt-pay-int-002", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(patronId).orElseThrow();
            assertThat(updated.getOutstandingFines()).isEqualByComparingTo(BigDecimal.valueOf(30));
            assertThat(updated.getStatus().name()).isEqualTo("ACTIVE");
        });
    }
}
