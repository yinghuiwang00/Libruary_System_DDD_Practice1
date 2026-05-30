package com.library.payment.domain.model;

import com.library.payment.domain.exception.InvalidOperationException;
import com.library.payment.domain.model.enums.RefundStatus;
import com.library.shared.domain.model.RefundId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class RefundTest {

    private static final RefundId REFUND_ID = RefundId.generate();
    private static final String PAYMENT_ID = "payment-uuid-1234";
    private static final BigDecimal AMOUNT = new BigDecimal("50.00");
    private static final String REASON = "Customer requested refund";

    // =========================================================================
    // Creation
    // =========================================================================

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create refund with valid parameters")
        void shouldCreateRefundWithValidParameters() {
            Refund refund = Refund.create(REFUND_ID, PAYMENT_ID, AMOUNT, REASON);

            assertThat(refund).isNotNull();
            assertThat(refund.getId()).isEqualTo(REFUND_ID);
            assertThat(refund.getPaymentId()).isEqualTo(PAYMENT_ID);
            assertThat(refund.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(refund.getReason()).isEqualTo(REASON);
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
            assertThat(refund.getRequestedDate()).isNotNull();
            assertThat(refund.getProcessedDate()).isNull();
            assertThat(refund.getExternalRefundId()).isNull();
            assertThat(refund.getRefundMethod()).isNull();
        }

        @Test
        @DisplayName("should create refund with null reason")
        void shouldCreateRefundWithNullReason() {
            Refund refund = Refund.create(REFUND_ID, PAYMENT_ID, AMOUNT, null);

            assertThat(refund.getReason()).isNull();
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        }

        @Test
        @DisplayName("should reject null refund ID")
        void shouldRejectNullRefundId() {
            assertThatThrownBy(() -> Refund.create(null, PAYMENT_ID, AMOUNT, REASON))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Refund ID must not be null");
        }

        @Test
        @DisplayName("should reject null payment ID")
        void shouldRejectNullPaymentId() {
            assertThatThrownBy(() -> Refund.create(REFUND_ID, null, AMOUNT, REASON))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Payment ID must not be null");
        }

        @Test
        @DisplayName("should reject null amount")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() -> Refund.create(REFUND_ID, PAYMENT_ID, null, REASON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refund amount must not be null");
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            assertThatThrownBy(() -> Refund.create(REFUND_ID, PAYMENT_ID, BigDecimal.ZERO, REASON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refund amount must be positive");
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThatThrownBy(() -> Refund.create(REFUND_ID, PAYMENT_ID, new BigDecimal("-5.00"), REASON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refund amount must be positive");
        }

        @Test
        @DisplayName("should generate unique refund IDs")
        void shouldGenerateUniqueRefundIds() {
            RefundId id1 = RefundId.generate();
            RefundId id2 = RefundId.generate();

            assertThat(id1).isNotEqualTo(id2);
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
            Refund refund = createPendingRefund();
            String externalRefundId = "EXT-REF-001";

            refund.process(externalRefundId);

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
            assertThat(refund.getExternalRefundId()).isEqualTo(externalRefundId);
        }

        @Test
        @DisplayName("should reject process when not PENDING")
        void shouldRejectProcessWhenNotPending() {
            Refund refund = createPendingRefund();
            refund.process("EXT-1");

            assertThatThrownBy(() -> refund.process("EXT-2"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot process refund in status: PROCESSING");
        }

        @Test
        @DisplayName("should reject process when COMPLETED")
        void shouldRejectProcessWhenCompleted() {
            Refund refund = createCompletedRefund();

            assertThatThrownBy(() -> refund.process("EXT-3"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot process refund in status: COMPLETED");
        }

        @Test
        @DisplayName("should reject process when FAILED")
        void shouldRejectProcessWhenFailed() {
            Refund refund = createFailedRefund();

            assertThatThrownBy(() -> refund.process("EXT-4"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot process refund in status: FAILED");
        }

        @Test
        @DisplayName("should reject process when CANCELLED")
        void shouldRejectProcessWhenCancelled() {
            Refund refund = createCancelledRefund();

            assertThatThrownBy(() -> refund.process("EXT-5"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot process refund in status: CANCELLED");
        }
    }

    // =========================================================================
    // Complete
    // =========================================================================

    @Nested
    @DisplayName("Complete")
    class Complete {

        @Test
        @DisplayName("should transition PROCESSING to COMPLETED and set fields")
        void shouldTransitionProcessingToCompleted() {
            Refund refund = createProcessingRefund();
            String method = "ORIGINAL_METHOD";

            refund.complete(method);

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
            assertThat(refund.getProcessedDate()).isNotNull();
            assertThat(refund.getRefundMethod()).isEqualTo(method);
        }

        @Test
        @DisplayName("should reject complete when PENDING")
        void shouldRejectCompleteWhenPending() {
            Refund refund = createPendingRefund();

            assertThatThrownBy(() -> refund.complete("method"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot complete refund in status: PENDING");
        }

        @Test
        @DisplayName("should reject complete when already COMPLETED")
        void shouldRejectCompleteWhenAlreadyCompleted() {
            Refund refund = createCompletedRefund();

            assertThatThrownBy(() -> refund.complete("method"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot complete refund in status: COMPLETED");
        }

        @Test
        @DisplayName("should reject complete when FAILED")
        void shouldRejectCompleteWhenFailed() {
            Refund refund = createFailedRefund();

            assertThatThrownBy(() -> refund.complete("method"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot complete refund in status: FAILED");
        }
    }

    // =========================================================================
    // Fail
    // =========================================================================

    @Nested
    @DisplayName("Fail")
    class Fail {

        @Test
        @DisplayName("should transition PROCESSING to FAILED")
        void shouldTransitionProcessingToFailed() {
            Refund refund = createProcessingRefund();

            refund.fail();

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
        }

        @Test
        @DisplayName("should reject fail when PENDING")
        void shouldRejectFailWhenPending() {
            Refund refund = createPendingRefund();

            assertThatThrownBy(refund::fail)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot fail refund in status: PENDING");
        }

        @Test
        @DisplayName("should reject fail when COMPLETED")
        void shouldRejectFailWhenCompleted() {
            Refund refund = createCompletedRefund();

            assertThatThrownBy(refund::fail)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot fail refund in status: COMPLETED");
        }

        @Test
        @DisplayName("should reject fail when CANCELLED")
        void shouldRejectFailWhenCancelled() {
            Refund refund = createCancelledRefund();

            assertThatThrownBy(refund::fail)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot fail refund in status: CANCELLED");
        }

        @Test
        @DisplayName("should reject fail when already FAILED")
        void shouldRejectFailWhenAlreadyFailed() {
            Refund refund = createFailedRefund();

            assertThatThrownBy(refund::fail)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot fail refund in status: FAILED");
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
            Refund refund = createPendingRefund();

            refund.cancel();

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.CANCELLED);
        }

        @Test
        @DisplayName("should reject cancel when PROCESSING")
        void shouldRejectCancelWhenProcessing() {
            Refund refund = createProcessingRefund();

            assertThatThrownBy(refund::cancel)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot cancel refund in status: PROCESSING");
        }

        @Test
        @DisplayName("should reject cancel when COMPLETED")
        void shouldRejectCancelWhenCompleted() {
            Refund refund = createCompletedRefund();

            assertThatThrownBy(refund::cancel)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot cancel refund in status: COMPLETED");
        }

        @Test
        @DisplayName("should reject cancel when FAILED")
        void shouldRejectCancelWhenFailed() {
            Refund refund = createFailedRefund();

            assertThatThrownBy(refund::cancel)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot cancel refund in status: FAILED");
        }

        @Test
        @DisplayName("should reject cancel when already CANCELLED")
        void shouldRejectCancelWhenAlreadyCancelled() {
            Refund refund = createCancelledRefund();

            assertThatThrownBy(refund::cancel)
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot cancel refund in status: CANCELLED");
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
            Refund refund = createPendingRefund();
            assertThat(refund.isPending()).isTrue();
        }

        @Test
        @DisplayName("isPending returns false for non-PENDING status")
        void isPendingReturnsFalseForNonPending() {
            Refund refund = createCompletedRefund();
            assertThat(refund.isPending()).isFalse();
        }

        @Test
        @DisplayName("isCompleted returns true for COMPLETED status")
        void isCompletedReturnsTrueForCompleted() {
            Refund refund = createCompletedRefund();
            assertThat(refund.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("isCompleted returns false for non-COMPLETED status")
        void isCompletedReturnsFalseForNonCompleted() {
            Refund refund = createPendingRefund();
            assertThat(refund.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("isFailed returns true for FAILED status")
        void isFailedReturnsTrueForFailed() {
            Refund refund = createFailedRefund();
            assertThat(refund.isFailed()).isTrue();
        }

        @Test
        @DisplayName("isFailed returns false for non-FAILED status")
        void isFailedReturnsFalseForNonFailed() {
            Refund refund = createPendingRefund();
            assertThat(refund.isFailed()).isFalse();
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
            Refund refund = createPendingRefund();

            refund.process("EXT-REFUND-001");
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
            assertThat(refund.getExternalRefundId()).isEqualTo("EXT-REFUND-001");

            refund.complete("ORIGINAL_METHOD");
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
            assertThat(refund.getProcessedDate()).isNotNull();
            assertThat(refund.getRefundMethod()).isEqualTo("ORIGINAL_METHOD");
        }

        @Test
        @DisplayName("should support PENDING -> PROCESSING -> FAILED lifecycle")
        void shouldSupportFailedLifecycle() {
            Refund refund = createPendingRefund();

            refund.process("EXT-REFUND-002");
            refund.fail();
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
        }

        @Test
        @DisplayName("should support PENDING -> CANCELLED lifecycle")
        void shouldSupportCancelledLifecycle() {
            Refund refund = createPendingRefund();

            refund.cancel();
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.CANCELLED);
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Refund createPendingRefund() {
        return Refund.create(REFUND_ID, PAYMENT_ID, AMOUNT, REASON);
    }

    private Refund createProcessingRefund() {
        Refund refund = createPendingRefund();
        refund.process("EXT-REFUND-PROC");
        return refund;
    }

    private Refund createCompletedRefund() {
        Refund refund = createProcessingRefund();
        refund.complete("BANK_TRANSFER");
        return refund;
    }

    private Refund createFailedRefund() {
        Refund refund = createProcessingRefund();
        refund.fail();
        return refund;
    }

    private Refund createCancelledRefund() {
        Refund refund = createPendingRefund();
        refund.cancel();
        return refund;
    }
}
