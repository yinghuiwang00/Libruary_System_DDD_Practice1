package com.library.integration.inventory;

import com.library.integration.BaseEndToEndTest;
import com.library.notification.domain.model.Notification;
import com.library.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * UC-8: Low Stock Alert End-to-End Test
 *
 * Flow: Inventory publishes LowStockAlertEvent →
 *   - Notification creates low stock alert notification (sent to LIBRARIAN)
 *
 * Note: LowStockAnalyticsHandler currently only logs, so we don't verify AnalyticsReport creation.
 */
@DirtiesContext
class LowStockAlertEndToEndTest extends BaseEndToEndTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        setUpKafka();
        cleanAllTables();
    }

    @Test
    void shouldCreateNotification_whenLowStockAlertEvent() {
        String eventJson = buildEventJson("LowStockAlertEvent",
            "inventoryId", jsonString("inv-001"),
            "bookId", jsonString("book-lowstock-001"),
            "availableCopies", "1",
            "threshold", "2",
            "alertedAt", jsonString("2026-05-31T10:00:00")
        );

        sendEvent(INVENTORY_TOPIC, eventJson);

        // Notification should be created (handler sends to "LIBRARIAN")
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId("LIBRARIAN");
            assertThat(notifications).isNotEmpty();
        });
    }
}
