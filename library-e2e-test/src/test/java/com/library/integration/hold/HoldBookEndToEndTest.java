package com.library.integration.hold;

import com.library.integration.BaseEndToEndTest;
import com.library.notification.domain.model.Notification;
import com.library.notification.domain.repository.NotificationRepository;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * UC-3: Hold Book End-to-End Test
 *
 * Flow: Circulation publishes HoldPlacedEvent / HoldFulfilledEvent →
 *   - Notification creates hold-related notification
 */
@DirtiesContext
class HoldBookEndToEndTest extends BaseEndToEndTest {

    @Autowired
    private PatronRepository patronRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Patron testPatron;

    @BeforeEach
    void setUp() {
        setUpKafka();
        cleanAllTables();

        testPatron = Patron.create("Bob", "Johnson", "bob.hold@test.com", PatronType.FACULTY);
        patronRepository.save(testPatron);
    }

    @Test
    void shouldCreateNotification_whenHoldPlacedEvent() {
        String patronId = testPatron.getId().getValue();
        String eventJson = buildEventJson("HoldPlacedEvent",
            "holdId", idObject("hold-001"),
            "bookId", idObject("book-001"),
            "patronId", idObject(patronId),
            "queuePosition", "1",
            "placedAt", jsonString("2026-05-31T10:00:00")
        );

        sendEvent(CIRCULATION_TOPIC, eventJson);

        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(patronId);
            assertThat(notifications).isNotEmpty();
        });
    }

    @Test
    void shouldCreateNotification_whenHoldFulfilledEvent() {
        String patronId = testPatron.getId().getValue();
        String eventJson = buildEventJson("HoldFulfilledEvent",
            "holdId", idObject("hold-001"),
            "bookId", idObject("book-001"),
            "patronId", idObject(patronId),
            "copyId", idObject("copy-001"),
            "availableUntil", jsonString("2026-06-07T10:00:00"),
            "fulfilledAt", jsonString("2026-05-31T10:00:00")
        );

        sendEvent(CIRCULATION_TOPIC, eventJson);

        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(patronId);
            assertThat(notifications).isNotEmpty();
        });
    }
}
