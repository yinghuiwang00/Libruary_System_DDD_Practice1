package com.library.integration.borrow;

import com.library.integration.BaseEndToEndTest;
import com.library.notification.domain.model.Notification;
import com.library.notification.domain.repository.NotificationRepository;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import com.library.shared.domain.model.PatronId;
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
 * UC-1: Borrow Book End-to-End Test
 *
 * Flow: Circulation publishes BookBorrowedEvent →
 *   - Patron updates loan count
 *   - Notification creates borrow notification
 */
@DirtiesContext
class BorrowBookEndToEndTest extends BaseEndToEndTest {

    @Autowired
    private PatronRepository patronRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Patron testPatron;

    @BeforeEach
    void setUp() {
        setUpKafka();
        cleanAllTables();

        // Create test patron
        testPatron = Patron.create("John", "Doe", "john.borrow@test.com", PatronType.STUDENT);
        patronRepository.save(testPatron);
    }

    @Test
    void shouldUpdatePatronLoanCountAndCreateNotification_whenBookBorrowedEvent() {
        // Given: a BookBorrowedEvent is published by Circulation
        String patronId = testPatron.getId().getValue();
        String eventJson = buildEventJson("BookBorrowedEvent",
            "loanId", idObject("loan-001"),
            "copyId", idObject("copy-001"),
            "patronId", idObject(patronId),
            "bookId", idObject("book-001"),
            "loanDate", jsonString("2026-05-31T10:00:00"),
            "dueDate", jsonString("2026-06-30T10:00:00")
        );

        // When: event is sent to circulation topic
        sendEvent(CIRCULATION_TOPIC, eventJson);

        // Then: patron's loan count should be incremented
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(testPatron.getId()).orElseThrow();
            assertThat(updated.getCurrentLoans()).isEqualTo(1);
            assertThat(updated.getTotalBorrowed()).isEqualTo(1);
        });

        // And: a notification should be created
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(patronId);
            assertThat(notifications).isNotEmpty();
        });
    }
}
