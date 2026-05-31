package com.library.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.domain.model.Notification;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.notification.domain.repository.NotificationRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"library.circulation.events"})
@ActiveProfiles("embedded-kafka")
@DirtiesContext
public class CirculationEventConsumerIntegrationTest {

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
    void should_create_borrow_notification_when_book_borrowed_event_received() throws Exception {
        String eventJson = """
            {
                "eventType": "BookBorrowedEvent",
                "eventId": "evt-borrow-001",
                "patronId": {"value": "patron-001"},
                "copyId": {"value": "copy-001"},
                "bookId": {"value": "book-001"},
                "loanId": {"value": "loan-001"}
            }
            """;

        kafkaTemplate.send("library.circulation.events", "evt-borrow-001", eventJson);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId("patron-001");
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).getNotificationType()).isEqualTo(NotificationType.BOOK_RETURNED);
            assertThat(notifications.get(0).getSubject()).isEqualTo("借书成功通知");
        });
    }

    @Test
    void should_create_return_notification_when_book_returned_event_received() throws Exception {
        String eventJson = """
            {
                "eventType": "BookReturnedEvent",
                "eventId": "evt-return-001",
                "patronId": {"value": "patron-002"},
                "copyId": {"value": "copy-002"},
                "bookId": {"value": "book-002"},
                "loanId": {"value": "loan-002"}
            }
            """;

        kafkaTemplate.send("library.circulation.events", "evt-return-001", eventJson);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId("patron-002");
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).getNotificationType()).isEqualTo(NotificationType.BOOK_RETURNED);
            assertThat(notifications.get(0).getSubject()).isEqualTo("还书成功通知");
        });
    }

    @Test
    void should_create_overdue_notification_when_overdue_event_received() throws Exception {
        String eventJson = """
            {
                "eventType": "OverdueNoticeEvent",
                "eventId": "evt-overdue-001",
                "patronId": {"value": "patron-003"},
                "daysOverdue": 5
            }
            """;

        kafkaTemplate.send("library.circulation.events", "evt-overdue-001", eventJson);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId("patron-003");
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).getNotificationType()).isEqualTo(NotificationType.OVERDUE_NOTICE);
            assertThat(notifications.get(0).getSubject()).isEqualTo("图书逾期通知");
            assertThat(notifications.get(0).getContent()).contains("5天");
        });
    }

    @Test
    void should_create_fine_notification_when_fine_event_received() throws Exception {
        String eventJson = """
            {
                "eventType": "FineIncurredEvent",
                "eventId": "evt-fine-001",
                "patronId": {"value": "patron-004"},
                "amount": "25.00",
                "fineId": {"value": "fine-001"}
            }
            """;

        kafkaTemplate.send("library.circulation.events", "evt-fine-001", eventJson);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId("patron-004");
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).getNotificationType()).isEqualTo(NotificationType.FINE_NOTIFICATION);
            assertThat(notifications.get(0).getSubject()).isEqualTo("罚款通知");
            assertThat(notifications.get(0).getContent()).contains("25.00");
        });
    }
}
