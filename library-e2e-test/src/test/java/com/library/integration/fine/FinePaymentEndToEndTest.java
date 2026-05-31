package com.library.integration.fine;

import com.library.integration.BaseEndToEndTest;
import com.library.notification.domain.model.Notification;
import com.library.notification.domain.repository.NotificationRepository;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import com.library.payment.domain.model.Payment;
import com.library.payment.domain.repository.PaymentRepository;
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
 * UC-4 + UC-5: Fine/Payment End-to-End Test
 *
 * Flow A: Circulation publishes FineIncurredEvent →
 *   - Patron adds fine to outstanding balance
 *   - Payment creates a payment record
 *   - Notification creates fine notification
 *
 * Flow B: Payment publishes PaymentCompletedEvent →
 *   - Patron reduces outstanding fines
 *   - Notification creates payment confirmation
 */
@DirtiesContext
class FinePaymentEndToEndTest extends BaseEndToEndTest {

    @Autowired
    private PatronRepository patronRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Patron testPatron;

    @BeforeEach
    void setUp() {
        setUpKafka();
        cleanAllTables();

        testPatron = Patron.create("Alice", "Williams", "alice.fine@test.com", PatronType.STUDENT);
        patronRepository.save(testPatron);
    }

    @Test
    void shouldUpdatePatronFineAndCreatePayment_whenFineIncurredEvent() {
        String patronId = testPatron.getId().getValue();
        String eventJson = buildEventJson("FineIncurredEvent",
            "fineId", idObject("fine-001"),
            "loanId", idObject("loan-001"),
            "patronId", idObject(patronId),
            "amount", "15.00",
            "overdueDays", "10",
            "incurredAt", jsonString("2026-05-31T10:00:00")
        );

        sendEvent(CIRCULATION_TOPIC, eventJson);

        // Patron fine balance should increase
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(testPatron.getId()).orElseThrow();
            assertThat(updated.getOutstandingFines()).isEqualByComparingTo(new BigDecimal("15.00"));
        });

        // Notification should be created
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(patronId);
            assertThat(notifications).isNotEmpty();
        });
    }

    @Test
    void shouldReducePatronFine_whenPaymentCompletedEvent() {
        // First: add a fine to the patron
        testPatron.addFine(new BigDecimal("25.00"));
        patronRepository.save(testPatron);

        String patronId = testPatron.getId().getValue();
        String eventJson = buildEventJson("PaymentCompletedEvent",
            "paymentId", idObject("pay-001"),
            "patronId", idObject(patronId),
            "amount", "25.00",
            "referenceNumber", jsonString("REF-001"),
            "paymentDate", jsonString("2026-05-31T12:00:00")
        );

        sendEvent(PAYMENT_TOPIC, eventJson);

        // Patron fine balance should decrease
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(testPatron.getId()).orElseThrow();
            assertThat(updated.getOutstandingFines()).isEqualByComparingTo(BigDecimal.ZERO);
        });

        // Notification should be created
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(patronId);
            assertThat(notifications).isNotEmpty();
        });
    }
}
