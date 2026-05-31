package com.library.integration.patron;

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
 * UC-7: Patron Status Change End-to-End Test
 *
 * Flow: Patron publishes PatronSuspendedEvent →
 *   - Notification creates status change notification
 */
@DirtiesContext
class PatronSuspensionEndToEndTest extends BaseEndToEndTest {

    @Autowired
    private PatronRepository patronRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Patron testPatron;

    @BeforeEach
    void setUp() {
        setUpKafka();
        cleanAllTables();

        testPatron = Patron.create("Tom", "Brown", "tom.suspend@test.com", PatronType.STUDENT);
        patronRepository.save(testPatron);
    }

    @Test
    void shouldCreateNotification_whenPatronSuspendedEvent() {
        String patronId = testPatron.getId().getValue();
        String eventJson = buildEventJson("PatronSuspendedEvent",
            "patronId", idObject(patronId),
            "reason", jsonString("Excessive overdue books")
        );

        sendEvent(PATRON_TOPIC, eventJson);

        // Notification should be created for the patron
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(patronId);
            assertThat(notifications).isNotEmpty();
        });
    }
}
