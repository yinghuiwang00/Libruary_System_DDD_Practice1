package com.library.payment.domain.model;

import com.library.payment.domain.exception.InvalidOperationException;
import com.library.payment.domain.model.enums.PaymentMethod;
import com.library.payment.domain.model.enums.PaymentStatus;
import com.library.payment.domain.model.enums.PaymentType;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.PaymentId;
import com.library.shared.domain.model.RefundId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PaymentTest {

    private static final PatronId PATRON_ID = PatronId.generate();
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final PaymentMethod PAYMENT_METHOD = PaymentMethod.ALIPAY;
    private static final PaymentType PAYMENT_TYPE = PaymentType.FINE_PAYMENT;
    private static final String DESCRIPTION = "Test payment";

    // =========================================================================
    // Creation
    // =========================================================================

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create payment with valid parameters")
        void shouldCreatePaymentWithValidParameters() {
            Payment payment = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);

            assertThat(payment).isNotNull();
            assertThat(payment.getId()).isNotNull();
            assertThat(payment.getPatronId()).isEqualTo(PATRON_ID);
            assertThat(payment.getPaymentType()).isEqualTo(PAYMENT_TYPE);
            assertThat(payment.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(payment.getPaymentMethod()).isEqualTo(PAYMENT_METHOD);
            assertThat(payment.getDescription()).isEqualTo(DESCRIPTION);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getCurrency()).isEqualTo("CNY");
            assertThat(payment.getFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(payment.getNetAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(payment.getReferenceNumber()).isNotNull();
            assertThat(payment.getPaymentDate()).isNull();
            assertThat(payment.getProcessedDate()).isNull();
            assertThat(payment.getExternalTransactionId()).isNull();
            assertThat(payment.getFailureReason()).isNull();
            assertThat(payment.getRefunds()).isEmpty();
        }

        @Test
        @DisplayName("should create payment with null description")
        void shouldCreatePaymentWithNullDescription() {
            Payment payment = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, null);

            assertThat(payment.getDescription()).isNull();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("should reject null patron ID")
        void shouldRejectNullPatronId() {
            assertThatThrownBy(() -> Payment.create(null, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Patron ID must not be null");
        }

        @Test
        @DisplayName("should reject null payment type")
        void shouldRejectNullPaymentType() {
            assertThatThrownBy(() -> Payment.create(PATRON_ID, null, AMOUNT, PAYMENT_METHOD, DESCRIPTION))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Payment type must not be null");
        }

        @Test
        @DisplayName("should reject null amount")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() -> Payment.create(PATRON_ID, PAYMENT_TYPE, null, PAYMENT_METHOD, DESCRIPTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment amount must not be null");
        }

        @Test
        @DisplayName("should reject null payment method")
        void shouldRejectNullPaymentMethod() {
            assertThatThrownBy(() -> Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, null, DESCRIPTION))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Payment method must not be null");
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            assertThatThrownBy(() -> Payment.create(PATRON_ID, PAYMENT_TYPE, BigDecimal.ZERO, PAYMENT_METHOD, DESCRIPTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment amount must be positive");
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThatThrownBy(() -> Payment.create(PATRON_ID, PAYMENT_TYPE, new BigDecimal("-10.00"), PAYMENT_METHOD, DESCRIPTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment amount must be positive");
        }

        @Test
        @DisplayName("should scale amount to 2 decimal places")
        void shouldScaleAmountTo2DecimalPlaces() {
            Payment payment = Payment.create(PATRON_ID, PAYMENT_TYPE, new BigDecimal("99.999"), PAYMENT_METHOD, DESCRIPTION);

            assertThat(payment.getAmount().scale()).isEqualTo(2);
            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should generate unique IDs for different payments")
        void shouldGenerateUniqueIds() {
            Payment payment1 = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);
            Payment payment2 = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);

            assertThat(payment1.getId()).isNotEqualTo(payment2.getId());
        }

        @Test
        @DisplayName("should generate unique reference numbers for different payments")
        void shouldGenerateUniqueReferenceNumbers() {
            Payment payment1 = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);
            Payment payment2 = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);

            assertThat(payment1.getReferenceNumber()).isNotEqualTo(payment2.getReferenceNumber());
        }
    }

    // =========================================================================
    // Reference Number Format
    // =========================================================================

    @Nested
    @DisplayName("Reference Number")
    class ReferenceNumber {

        @Test
        @DisplayName("should start with PAY prefix")
        void shouldStartWithPayPrefix() {
            Payment payment = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);

            assertThat(payment.getReferenceNumber()).startsWith("PAY");
        }

        @Test
        @DisplayName("should have format PAY + timestamp + 4-digit suffix")
        void shouldHaveCorrectFormat() {
            Payment payment = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);

            String ref = payment.getReferenceNumber();
            assertThat(ref).matches("PAY\\d{14}\\d{4}");
            assertThat(ref).hasSize(21); // PAY(3) + 14-digit timestamp + 4-digit suffix
        }
    }

    // =========================================================================
    // Process
    // =========================================================================

    @Nested
    @DisplayName("Process")
    class Process {

        @Test
        @DisplayName("should transition PENDING to PROCESSING")
        void shouldTransitionPendingToProcessing() {
            Payment payment = createPendingPayment();
            String txId = "TXN-12345";

            payment.process(txId);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
            assertThat(payment.getExternalTransactionId()).isEqualTo(txId);
        }

        @Test
        @DisplayName("should reject process when not PENDING")
        void shouldRejectProcessWhenNotPending() {
            Payment payment = createPendingPayment();
            payment.process("TXN-1");

            assertThatThrownBy(() -> payment.process("TXN-2"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot process payment in status: PROCESSING");
        }

        @Test
        @DisplayName("should reject process when COMPLETED")
        void shouldRejectProcessWhenCompleted() {
            Payment payment = createCompletedPayment();

            assertThatThrownBy(() -> payment.process("TXN-3"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot process payment in status: COMPLETED");
        }

        @Test
        @DisplayName("should reject process when FAILED")
        void shouldRejectProcessWhenFailed() {
            Payment payment = createFailedPayment();

            assertThatThrownBy(() -> payment.process("TXN-4"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot process payment in status: FAILED");
        }

        @Test
        @DisplayName("should reject process when CANCELLED")
        void shouldRejectProcessWhenCancelled() {
            Payment payment = createCancelledPayment();

            assertThatThrownBy(() -> payment.process("TXN-5"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot process payment in status: CANCELLED");
        }
    }

    // =========================================================================
    // Complete
    // =========================================================================

    @Nested
    @DisplayName("Complete")
    class Complete {

        @Test
        @DisplayName("should transition PROCESSING to COMPLETED and set dates")
        void shouldTransitionProcessingToCompleted() {
            Payment payment = createProcessingPayment();

            payment.complete();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getPaymentDate()).isNotNull();
            assertThat(payment.getProcessedDate()).isNotNull();
        }

        @Test
        @DisplayName("should reject complete when not PROCESSING")
        void shouldRejectCompleteWhenNotProcessing() {
            Payment payment = createPendingPayment();

            assertThatThrownBy(payment::complete)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot complete payment in status: PENDING");
        }

        @Test
        @DisplayName("should reject complete when already COMPLETED")
        void shouldRejectCompleteWhenAlreadyCompleted() {
            Payment payment = createCompletedPayment();

            assertThatThrownBy(payment::complete)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot complete payment in status: COMPLETED");
        }
    }

    // =========================================================================
    // Fail
    // =========================================================================

    @Nested
    @DisplayName("Fail")
    class Fail {

        @Test
        @DisplayName("should transition PROCESSING to FAILED and set failure reason")
        void shouldTransitionProcessingToFailed() {
            Payment payment = createProcessingPayment();
            String reason = "Insufficient funds";

            payment.fail(reason);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("should reject fail when PENDING")
        void shouldRejectFailWhenPending() {
            Payment payment = createPendingPayment();

            assertThatThrownBy(() -> payment.fail("some reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot fail payment in status: PENDING");
        }

        @Test
        @DisplayName("should reject fail when COMPLETED")
        void shouldRejectFailWhenCompleted() {
            Payment payment = createCompletedPayment();

            assertThatThrownBy(() -> payment.fail("some reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot fail payment in status: COMPLETED");
        }

        @Test
        @DisplayName("should reject fail when REFUNDED")
        void shouldRejectFailWhenRefunded() {
            Payment payment = createRefundedPayment();

            assertThatThrownBy(() -> payment.fail("some reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot fail payment in status: REFUNDED");
        }

        @Test
        @DisplayName("should accept null failure reason")
        void shouldAcceptNullFailureReason() {
            Payment payment = createProcessingPayment();

            payment.fail(null);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isNull();
        }
    }

    // =========================================================================
    // Cancel
    // =========================================================================

    @Nested
    @DisplayName("Cancel")
    class Cancel {

        @Test
        @DisplayName("should transition PENDING to CANCELLED")
        void shouldTransitionPendingToCancelled() {
            Payment payment = createPendingPayment();
            String reason = "User requested cancellation";

            payment.cancel(reason);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(payment.getFailureReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("should reject cancel when PROCESSING")
        void shouldRejectCancelWhenProcessing() {
            Payment payment = createProcessingPayment();

            assertThatThrownBy(() -> payment.cancel("reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot cancel payment in status: PROCESSING");
        }

        @Test
        @DisplayName("should reject cancel when COMPLETED")
        void shouldRejectCancelWhenCompleted() {
            Payment payment = createCompletedPayment();

            assertThatThrownBy(() -> payment.cancel("reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot cancel payment in status: COMPLETED");
        }

        @Test
        @DisplayName("should reject cancel when FAILED")
        void shouldRejectCancelWhenFailed() {
            Payment payment = createFailedPayment();

            assertThatThrownBy(() -> payment.cancel("reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot cancel payment in status: FAILED");
        }
    }

    // =========================================================================
    // Request Refund
    // =========================================================================

    @Nested
    @DisplayName("Request Refund")
    class RequestRefund {

        @Test
        @DisplayName("should create refund and change status to REFUNDED on full refund")
        void shouldCreateRefundAndChangeStatusOnFullRefund() {
            Payment payment = createCompletedPayment();
            RefundId refundId = RefundId.generate();

            Refund refund = payment.requestRefund(refundId, AMOUNT, "Customer complaint");

            assertThat(refund).isNotNull();
            assertThat(refund.getId()).isEqualTo(refundId);
            assertThat(refund.getPaymentId()).isEqualTo(payment.getId().getValue());
            assertThat(refund.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(refund.getReason()).isEqualTo("Customer complaint");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.getRefunds()).hasSize(1);
            assertThat(payment.getRefunds().get(0)).isSameAs(refund);
        }

        @Test
        @DisplayName("should keep COMPLETED status on partial refund")
        void shouldKeepCompletedStatusOnPartialRefund() {
            Payment payment = createCompletedPayment();
            BigDecimal partialAmount = new BigDecimal("50.00");

            Refund refund = payment.requestRefund(RefundId.generate(), partialAmount, "Partial refund");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(refund.getAmount()).isEqualByComparingTo(partialAmount);
        }

        @Test
        @DisplayName("should reject refund when PENDING")
        void shouldRejectRefundWhenPending() {
            Payment payment = createPendingPayment();

            assertThatThrownBy(() -> payment.requestRefund(RefundId.generate(), AMOUNT, "reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Payment cannot be refunded in status: PENDING");
        }

        @Test
        @DisplayName("should reject refund when PROCESSING")
        void shouldRejectRefundWhenProcessing() {
            Payment payment = createProcessingPayment();

            assertThatThrownBy(() -> payment.requestRefund(RefundId.generate(), AMOUNT, "reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Payment cannot be refunded in status: PROCESSING");
        }

        @Test
        @DisplayName("should reject refund when FAILED")
        void shouldRejectRefundWhenFailed() {
            Payment payment = createFailedPayment();

            assertThatThrownBy(() -> payment.requestRefund(RefundId.generate(), AMOUNT, "reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Payment cannot be refunded in status: FAILED");
        }

        @Test
        @DisplayName("should reject refund when already REFUNDED")
        void shouldRejectRefundWhenAlreadyRefunded() {
            Payment payment = createRefundedPayment();

            assertThatThrownBy(() -> payment.requestRefund(RefundId.generate(), AMOUNT, "reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Payment cannot be refunded in status: REFUNDED");
        }

        @Test
        @DisplayName("should reject refund amount exceeding payment amount")
        void shouldRejectRefundExceedingPaymentAmount() {
            Payment payment = createCompletedPayment();
            BigDecimal excessAmount = new BigDecimal("200.00");

            assertThatThrownBy(() -> payment.requestRefund(RefundId.generate(), excessAmount, "reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("exceeds refundable amount");
        }

        @Test
        @DisplayName("should support multiple partial refunds")
        void shouldSupportMultiplePartialRefunds() {
            Payment payment = createCompletedPaymentWithAmount(new BigDecimal("100.00"));

            Refund refund1 = payment.requestRefund(RefundId.generate(), new BigDecimal("30.00"), "First partial");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

            completeRefund(refund1);

            // The second refund checks refundable amount against completed refunds
            Refund refund2 = payment.requestRefund(RefundId.generate(), new BigDecimal("70.00"), "Second partial");

            // Status remains COMPLETED because requestRefund only transitions to REFUNDED
            // when the individual refund amount equals the total payment amount
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getRefunds()).hasSize(2);
        }

        @Test
        @DisplayName("should reject refund when completed refunds exceed remaining amount")
        void shouldRejectRefundWhenCompletedRefundsExceedRemaining() {
            Payment payment = createCompletedPaymentWithAmount(new BigDecimal("100.00"));

            Refund refund1 = payment.requestRefund(RefundId.generate(), new BigDecimal("80.00"), "First");
            completeRefund(refund1);

            // 80 already refunded, only 20 remaining, so 30 should exceed
            assertThatThrownBy(() -> payment.requestRefund(RefundId.generate(), new BigDecimal("30.00"), "Excess"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("exceeds refundable amount");
        }
    }

    // =========================================================================
    // Status Check Methods
    // =========================================================================

    @Nested
    @DisplayName("Status Check Methods")
    class StatusCheckMethods {

        @Test
        @DisplayName("isPending returns true for PENDING status")
        void isPendingReturnsTrueForPending() {
            Payment payment = createPendingPayment();
            assertThat(payment.isPending()).isTrue();
        }

        @Test
        @DisplayName("isPending returns false for non-PENDING status")
        void isPendingReturnsFalseForNonPending() {
            Payment payment = createCompletedPayment();
            assertThat(payment.isPending()).isFalse();
        }

        @Test
        @DisplayName("isCompleted returns true for COMPLETED status")
        void isCompletedReturnsTrueForCompleted() {
            Payment payment = createCompletedPayment();
            assertThat(payment.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("isCompleted returns false for non-COMPLETED status")
        void isCompletedReturnsFalseForNonCompleted() {
            Payment payment = createPendingPayment();
            assertThat(payment.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("isFailed returns true for FAILED status")
        void isFailedReturnsTrueForFailed() {
            Payment payment = createFailedPayment();
            assertThat(payment.isFailed()).isTrue();
        }

        @Test
        @DisplayName("isFailed returns false for non-FAILED status")
        void isFailedReturnsFalseForNonFailed() {
            Payment payment = createPendingPayment();
            assertThat(payment.isFailed()).isFalse();
        }

        @Test
        @DisplayName("isRefunded returns true for REFUNDED status")
        void isRefundedReturnsTrueForRefunded() {
            Payment payment = createRefundedPayment();
            assertThat(payment.isRefunded()).isTrue();
        }

        @Test
        @DisplayName("isRefunded returns false for non-REFUNDED status")
        void isRefundedReturnsFalseForNonRefunded() {
            Payment payment = createPendingPayment();
            assertThat(payment.isRefunded()).isFalse();
        }
    }

    // =========================================================================
    // canBeRefunded and getRefundableAmount
    // =========================================================================

    @Nested
    @DisplayName("Refund Eligibility")
    class RefundEligibility {

        @Test
        @DisplayName("canBeRefunded returns true for COMPLETED payment")
        void canBeRefundedReturnsTrueForCompleted() {
            Payment payment = createCompletedPayment();
            assertThat(payment.canBeRefunded()).isTrue();
        }

        @Test
        @DisplayName("canBeRefunded returns false for PENDING payment")
        void canBeRefundedReturnsFalseForPending() {
            Payment payment = createPendingPayment();
            assertThat(payment.canBeRefunded()).isFalse();
        }

        @Test
        @DisplayName("canBeRefunded returns false for PROCESSING payment")
        void canBeRefundedReturnsFalseForProcessing() {
            Payment payment = createProcessingPayment();
            assertThat(payment.canBeRefunded()).isFalse();
        }

        @Test
        @DisplayName("canBeRefunded returns false for FAILED payment")
        void canBeRefundedReturnsFalseForFailed() {
            Payment payment = createFailedPayment();
            assertThat(payment.canBeRefunded()).isFalse();
        }

        @Test
        @DisplayName("canBeRefunded returns false for CANCELLED payment")
        void canBeRefundedReturnsFalseForCancelled() {
            Payment payment = createCancelledPayment();
            assertThat(payment.canBeRefunded()).isFalse();
        }

        @Test
        @DisplayName("canBeRefunded returns false for REFUNDED payment")
        void canBeRefundedReturnsFalseForRefunded() {
            Payment payment = createRefundedPayment();
            assertThat(payment.canBeRefunded()).isFalse();
        }

        @Test
        @DisplayName("getRefundableAmount returns full amount when no refunds")
        void getRefundableAmountReturnsFullAmountWhenNoRefunds() {
            Payment payment = createCompletedPayment();
            assertThat(payment.getRefundableAmount()).isEqualByComparingTo(AMOUNT);
        }

        @Test
        @DisplayName("getRefundableAmount subtracts completed refunds")
        void getRefundableAmountSubtractsCompletedRefunds() {
            Payment payment = createCompletedPaymentWithAmount(new BigDecimal("100.00"));

            Refund refund = payment.requestRefund(RefundId.generate(), new BigDecimal("40.00"), "Partial");
            completeRefund(refund);

            assertThat(payment.getRefundableAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
        }

        @Test
        @DisplayName("getRefundableAmount ignores non-completed refunds")
        void getRefundableAmountIgnoresNonCompletedRefunds() {
            Payment payment = createCompletedPaymentWithAmount(new BigDecimal("100.00"));

            Refund refund = payment.requestRefund(RefundId.generate(), new BigDecimal("40.00"), "Partial");
            // refund is PENDING, not completed

            assertThat(payment.getRefundableAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("getRefundableAmount returns zero when fully refunded")
        void getRefundableAmountReturnsZeroWhenFullyRefunded() {
            Payment payment = createCompletedPaymentWithAmount(new BigDecimal("100.00"));

            Refund refund = payment.requestRefund(RefundId.generate(), new BigDecimal("100.00"), "Full");
            completeRefund(refund);

            assertThat(payment.getRefundableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // =========================================================================
    // Payment Types
    // =========================================================================

    @Nested
    @DisplayName("Payment Types")
    class PaymentTypes {

        @Test
        @DisplayName("should create FINE_PAYMENT")
        void shouldCreateFinePayment() {
            Payment payment = Payment.create(PATRON_ID, PaymentType.FINE_PAYMENT, AMOUNT, PAYMENT_METHOD, DESCRIPTION);
            assertThat(payment.getPaymentType()).isEqualTo(PaymentType.FINE_PAYMENT);
        }

        @Test
        @DisplayName("should create MEMBERSHIP_FEE")
        void shouldCreateMembershipFee() {
            Payment payment = Payment.create(PATRON_ID, PaymentType.MEMBERSHIP_FEE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);
            assertThat(payment.getPaymentType()).isEqualTo(PaymentType.MEMBERSHIP_FEE);
        }

        @Test
        @DisplayName("should create DEPOSIT")
        void shouldCreateDeposit() {
            Payment payment = Payment.create(PATRON_ID, PaymentType.DEPOSIT, AMOUNT, PAYMENT_METHOD, DESCRIPTION);
            assertThat(payment.getPaymentType()).isEqualTo(PaymentType.DEPOSIT);
        }

        @Test
        @DisplayName("should create OTHER type")
        void shouldCreateOtherType() {
            Payment payment = Payment.create(PATRON_ID, PaymentType.OTHER, AMOUNT, PAYMENT_METHOD, DESCRIPTION);
            assertThat(payment.getPaymentType()).isEqualTo(PaymentType.OTHER);
        }
    }

    // =========================================================================
    // Payment Methods
    // =========================================================================

    @Nested
    @DisplayName("Payment Methods")
    class PaymentMethods {

        @Test
        @DisplayName("should support all payment methods")
        void shouldSupportAllPaymentMethods() {
            for (PaymentMethod method : PaymentMethod.values()) {
                Payment payment = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, method, DESCRIPTION);
                assertThat(payment.getPaymentMethod()).isEqualTo(method);
            }
        }
    }

    // =========================================================================
    // Full Lifecycle
    // =========================================================================

    @Nested
    @DisplayName("Full Lifecycle")
    class FullLifecycle {

        @Test
        @DisplayName("should support PENDING -> PROCESSING -> COMPLETED lifecycle")
        void shouldSupportSuccessfulLifecycle() {
            Payment payment = createPendingPayment();

            payment.process("TXN-001");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);

            payment.complete();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getPaymentDate()).isNotNull();
            assertThat(payment.getProcessedDate()).isNotNull();
        }

        @Test
        @DisplayName("should support PENDING -> PROCESSING -> FAILED lifecycle")
        void shouldSupportFailedLifecycle() {
            Payment payment = createPendingPayment();

            payment.process("TXN-002");
            payment.fail("Bank declined");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo("Bank declined");
        }

        @Test
        @DisplayName("should support PENDING -> CANCELLED lifecycle")
        void shouldSupportCancelledLifecycle() {
            Payment payment = createPendingPayment();

            payment.cancel("User changed mind");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(payment.getFailureReason()).isEqualTo("User changed mind");
        }

        @Test
        @DisplayName("should support COMPLETED -> REFUNDED lifecycle")
        void shouldSupportRefundLifecycle() {
            Payment payment = createCompletedPayment();

            payment.requestRefund(RefundId.generate(), AMOUNT, "Full refund requested");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Payment createPendingPayment() {
        return Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);
    }

    private Payment createProcessingPayment() {
        Payment payment = createPendingPayment();
        payment.process("TXN-PROC");
        return payment;
    }

    private Payment createCompletedPayment() {
        Payment payment = createProcessingPayment();
        payment.complete();
        return payment;
    }

    private Payment createCompletedPaymentWithAmount(BigDecimal amount) {
        Payment payment = Payment.create(PATRON_ID, PAYMENT_TYPE, amount, PAYMENT_METHOD, DESCRIPTION);
        payment.process("TXN-PROC");
        payment.complete();
        return payment;
    }

    private Payment createFailedPayment() {
        Payment payment = createProcessingPayment();
        payment.fail("Some failure reason");
        return payment;
    }

    private Payment createCancelledPayment() {
        Payment payment = createPendingPayment();
        payment.cancel("Some cancel reason");
        return payment;
    }

    private Payment createRefundedPayment() {
        Payment payment = createCompletedPayment();
        payment.requestRefund(RefundId.generate(), AMOUNT, "Full refund");
        return payment;
    }

    private void completeRefund(Refund refund) {
        refund.process("EXT-REFUND-001");
        refund.complete("ORIGINAL_METHOD");
    }
}
