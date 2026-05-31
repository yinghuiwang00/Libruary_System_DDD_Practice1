package com.library.integration.returnn;

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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * UC-2: Return Book End-to-End Test
 *
 * Flow: Circulation publishes BookReturnedEvent →
 *   - Patron decrements loan count
 *   - Notification creates return notification
 */
@DirtiesContext
class ReturnBookEndToEndTest extends BaseEndToEndTest {

    @Autowired
    private PatronRepository patronRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Patron testPatron;

    @BeforeEach
    void setUp() {
        setUpKafka();
        cleanAllTables();

        // Create test patron with 1 active loan
        testPatron = Patron.create("Jane", "Smith", "jane.return@test.com", PatronType.STUDENT);
        testPatron.recordLoan();
        patronRepository.save(testPatron);
    }

    @Test
    void shouldDecrementPatronLoanCountAndCreateNotification_whenBookReturnedEvent() {
        // Given: a BookReturnedEvent is published by Circulation
        String patronId = testPatron.getId().getValue();
        String eventJson = buildEventJson("BookReturnedEvent",
            "loanId", idObject("loan-001"),
            "copyId", idObject("copy-001"),
            "patronId", idObject(patronId),
            "bookId", idObject("book-001"),
            "returnDate", jsonString("2026-06-15T10:00:00"),
            "fineAmount", "0"
        );

        // When: event is sent to circulation topic
        sendEvent(CIRCULATION_TOPIC, eventJson);

        // Then: patron's loan count should be decremented
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(testPatron.getId()).orElseThrow();
            assertThat(updated.getCurrentLoans()).isEqualTo(0);
        });

        // And: a notification should be created
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(patronId);
            assertThat(notifications).isNotEmpty();
        });
    }
}
