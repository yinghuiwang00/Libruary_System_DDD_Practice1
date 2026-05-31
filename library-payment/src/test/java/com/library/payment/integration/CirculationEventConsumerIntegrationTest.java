package com.library.payment.integration;

import com.library.payment.domain.model.Payment;
import com.library.payment.domain.model.enums.PaymentType;
import com.library.payment.domain.repository.PaymentRepository;
import com.library.shared.domain.model.PatronId;
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
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private PaymentRepository paymentRepository;

    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
        }

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(pf);
    }

    @Test
    void shouldCreatePaymentWhenFineIncurredEventReceived() {
        String patronId = "PATRON-FINE-001";
        String eventJson = """
            {
                "eventType": "FineIncurredEvent",
                "eventId": "evt-fine-001",
                "patronId": {"value": "%s"},
                "amount": "25.00",
                "overdueDays": 5
            }
            """.formatted(patronId);

        kafkaTemplate.send("library.circulation.events", "evt-fine-001", eventJson);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<Payment> payments = paymentRepository.findByPatronId(PatronId.of(patronId));
            assertThat(payments).hasSize(1);
            Payment payment = payments.get(0);
            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(payment.getPaymentType()).isEqualTo(PaymentType.FINE_PAYMENT);
        });
    }

    @Test
    void shouldIgnoreNonFineIncurredEvent() {
        String patronId = "PATRON-IGNORE-001";
        String eventJson = """
            {
                "eventType": "BookBorrowedEvent",
                "eventId": "evt-borrow-001",
                "patronId": {"value": "%s"}
            }
            """.formatted(patronId);

        kafkaTemplate.send("library.circulation.events", "evt-borrow-001", eventJson);

        await().atMost(3, SECONDS).pollDelay(2, SECONDS).untilAsserted(() -> {
            List<Payment> payments = paymentRepository.findByPatronId(PatronId.of(patronId));
            assertThat(payments).isEmpty();
        });
    }
}
