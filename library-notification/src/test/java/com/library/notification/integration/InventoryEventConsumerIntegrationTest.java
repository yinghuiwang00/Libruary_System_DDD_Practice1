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
@EmbeddedKafka(partitions = 1, topics = {"library.inventory.events"})
@ActiveProfiles("embedded-kafka")
@DirtiesContext
public class InventoryEventConsumerIntegrationTest {

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
    void should_create_low_stock_notification_when_low_stock_alert_received() throws Exception {
        String eventJson = """
            {
                "eventType": "LowStockAlertEvent",
                "eventId": "evt-lowstock-001",
                "bookId": "book-low-001",
                "availableCopies": 2,
                "libraryId": {"value": "lib-001"}
            }
            """;

        kafkaTemplate.send("library.inventory.events", "evt-lowstock-001", eventJson);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId("LIBRARIAN");
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).getNotificationType()).isEqualTo(NotificationType.SYSTEM_ANNOUNCEMENT);
            assertThat(notifications.get(0).getSubject()).isEqualTo("库存预警通知");
            assertThat(notifications.get(0).getContent()).contains("book-low-001");
            assertThat(notifications.get(0).getContent()).contains("2");
        });
    }
}
