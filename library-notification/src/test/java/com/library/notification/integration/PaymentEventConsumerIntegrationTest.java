package com.library.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.domain.model.Notification;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"library.payment.events"})
@ActiveProfiles("embedded-kafka")
@DirtiesContext
public class PaymentEventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            notificationRepository.deleteAll();
        });
    }

    @Test
    void should_create_payment_confirmation_notification_when_payment_completed_event_received() throws Exception {
        String eventJson = """
            {
                "eventType": "PaymentCompletedEvent",
                "eventId": "evt-payment-001",
                "patronId": {"value": "patron-pay-001"},
                "paymentId": {"value": "pay-001"},
                "amount": "50.00"
            }
            """;

        kafkaTemplate.send("library.payment.events", "evt-payment-001", eventJson);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId("patron-pay-001");
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).getNotificationType()).isEqualTo(NotificationType.PAYMENT_CONFIRMATION);
            assertThat(notifications.get(0).getSubject()).isEqualTo("支付确认通知");
            assertThat(notifications.get(0).getContent()).contains("50.00");
        });
    }
}
