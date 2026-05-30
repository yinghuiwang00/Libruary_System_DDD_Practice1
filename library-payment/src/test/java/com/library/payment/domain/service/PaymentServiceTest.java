package com.library.payment.domain.service;

import com.library.payment.domain.event.*;
import com.library.payment.domain.exception.InvalidOperationException;
import com.library.payment.domain.exception.PaymentNotFoundException;
import com.library.payment.domain.model.Payment;
import com.library.payment.domain.model.Refund;
import com.library.payment.domain.model.enums.PaymentMethod;
import com.library.payment.domain.model.enums.PaymentStatus;
import com.library.payment.domain.model.enums.PaymentType;
import com.library.payment.domain.repository.PaymentRepository;
import com.library.payment.domain.repository.RefundRepository;
import com.library.shared.domain.event.DomainEventPublisher;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.PaymentId;
import com.library.shared.domain.model.RefundId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private static final PatronId PATRON_ID = PatronId.generate();
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final PaymentMethod PAYMENT_METHOD = PaymentMethod.ALIPAY;
    private static final PaymentType PAYMENT_TYPE = PaymentType.FINE_PAYMENT;
    private static final String DESCRIPTION = "Test payment";

    // =========================================================================
    // createPayment
    // =========================================================================

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @Test
        @DisplayName("should create payment and publish event")
        void shouldCreatePaymentAndPublishEvent() {
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Payment result = paymentService.createPayment(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);

            assertThat(result).isNotNull();
            assertThat(result.getPatronId()).isEqualTo(PATRON_ID);
            assertThat(result.getPaymentType()).isEqualTo(PAYMENT_TYPE);
            assertThat(result.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(result.getPaymentMethod()).isEqualTo(PAYMENT_METHOD);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);

            verify(paymentRepository).save(any(Payment.class));

            ArgumentCaptor<PaymentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCreatedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            PaymentCreatedEvent event = eventCaptor.getValue();
            assertThat(event.getPaymentId()).isEqualTo(result.getId());
            assertThat(event.getPatronId()).isEqualTo(PATRON_ID);
            assertThat(event.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(event.getPaymentMethod()).isEqualTo(PAYMENT_METHOD);
        }

        @Test
        @DisplayName("should save and return the persisted payment")
        void shouldSaveAndReturnPersistedPayment() {
            Payment savedPayment = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

            Payment result = paymentService.createPayment(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, DESCRIPTION);

            assertThat(result).isSameAs(savedPayment);
        }
    }

    // =========================================================================
    // processPayment
    // =========================================================================

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("should process payment successfully")
        void shouldProcessPaymentSuccessfully() {
            Payment payment = createPendingPayment();
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            String txId = "TXN-PROC-001";
            Payment result = paymentService.processPayment(payment.getId(), txId);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
            assertThat(result.getExternalTransactionId()).isEqualTo(txId);
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processPayment(PaymentId.generate(), "TXN"))
                .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    // =========================================================================
    // completePayment
    // =========================================================================

    @Nested
    @DisplayName("completePayment")
    class CompletePayment {

        @Test
        @DisplayName("should complete payment and publish event")
        void shouldCompletePaymentAndPublishEvent() {
            Payment payment = createProcessingPayment();
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Payment result = paymentService.completePayment(payment.getId());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(result.getPaymentDate()).isNotNull();

            ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            PaymentCompletedEvent event = eventCaptor.getValue();
            assertThat(event.getPaymentId()).isEqualTo(result.getId());
            assertThat(event.getPatronId()).isEqualTo(PATRON_ID);
            assertThat(event.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(event.getReferenceNumber()).isEqualTo(result.getReferenceNumber());
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.completePayment(PaymentId.generate()))
                .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    // =========================================================================
    // failPayment
    // =========================================================================

    @Nested
    @DisplayName("failPayment")
    class FailPayment {

        @Test
        @DisplayName("should fail payment and publish event")
        void shouldFailPaymentAndPublishEvent() {
            Payment payment = createProcessingPayment();
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            String reason = "Bank declined";
            Payment result = paymentService.failPayment(payment.getId(), reason);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.getFailureReason()).isEqualTo(reason);

            ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            PaymentFailedEvent event = eventCaptor.getValue();
            assertThat(event.getPaymentId()).isEqualTo(result.getId());
            assertThat(event.getPatronId()).isEqualTo(PATRON_ID);
            assertThat(event.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(event.getFailureReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.failPayment(PaymentId.generate(), "reason"))
                .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    // =========================================================================
    // cancelPayment
    // =========================================================================

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("should cancel payment and publish event")
        void shouldCancelPaymentAndPublishEvent() {
            Payment payment = createPendingPayment();
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            String reason = "User cancelled";
            Payment result = paymentService.cancelPayment(payment.getId(), reason);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(result.getFailureReason()).isEqualTo(reason);

            ArgumentCaptor<PaymentCancelledEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCancelledEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            PaymentCancelledEvent event = eventCaptor.getValue();
            assertThat(event.getPaymentId()).isEqualTo(result.getId());
            assertThat(event.getPatronId()).isEqualTo(PATRON_ID);
            assertThat(event.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(event.getReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.cancelPayment(PaymentId.generate(), "reason"))
                .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    // =========================================================================
    // requestRefund
    // =========================================================================

    @Nested
    @DisplayName("requestRefund")
    class RequestRefund {

        @Test
        @DisplayName("should request refund and publish event")
        void shouldRequestRefundAndPublishEvent() {
            Payment payment = createCompletedPayment();
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.of(payment));
            when(refundRepository.findByPaymentId(anyString())).thenReturn(Collections.emptyList());
            when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            BigDecimal refundAmount = new BigDecimal("50.00");
            String reason = "Partial refund";
            Refund result = paymentService.requestRefund(payment.getId(), refundAmount, reason);

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualByComparingTo(refundAmount);
            assertThat(result.getReason()).isEqualTo(reason);

            verify(refundRepository).save(any(Refund.class));
            verify(paymentRepository).save(any(Payment.class));

            ArgumentCaptor<RefundRequestedEvent> eventCaptor = ArgumentCaptor.forClass(RefundRequestedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            RefundRequestedEvent event = eventCaptor.getValue();
            assertThat(event.getPaymentId()).isEqualTo(payment.getId());
            assertThat(event.getAmount()).isEqualByComparingTo(refundAmount);
            assertThat(event.getReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.requestRefund(PaymentId.generate(), AMOUNT, "reason"))
                .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("should reject refund exceeding persisted total")
        void shouldRejectRefundExceedingPersistedTotal() {
            Payment payment = createCompletedPayment();

            // Create a previously completed refund in the repository
            Refund existingRefund = Refund.create(RefundId.generate(), payment.getId().getValue(), AMOUNT, "Previous");
            existingRefund.process("EXT-OLD");
            existingRefund.complete("BANK_TRANSFER");

            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.of(payment));
            when(refundRepository.findByPaymentId(anyString())).thenReturn(List.of(existingRefund));

            assertThatThrownBy(() -> paymentService.requestRefund(payment.getId(), new BigDecimal("50.00"), "reason"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("exceeds refundable amount");
        }
    }

    // =========================================================================
    // processRefund
    // =========================================================================

    @Nested
    @DisplayName("processRefund")
    class ProcessRefund {

        @Test
        @DisplayName("should process refund successfully")
        void shouldProcessRefundSuccessfully() {
            Refund refund = Refund.create(RefundId.generate(), "payment-id", AMOUNT, "reason");
            when(refundRepository.findById(any(RefundId.class))).thenReturn(Optional.of(refund));
            when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

            String externalRefundId = "EXT-REF-001";
            Refund result = paymentService.processRefund(refund.getId(), externalRefundId);

            assertThat(result.getExternalRefundId()).isEqualTo(externalRefundId);
            verify(refundRepository).save(refund);
        }

        @Test
        @DisplayName("should throw when refund not found")
        void shouldThrowWhenRefundNotFound() {
            when(refundRepository.findById(any(RefundId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processRefund(RefundId.generate(), "EXT"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Refund not found");
        }
    }

    // =========================================================================
    // completeRefund
    // =========================================================================

    @Nested
    @DisplayName("completeRefund")
    class CompleteRefund {

        @Test
        @DisplayName("should complete refund and publish event")
        void shouldCompleteRefundAndPublishEvent() {
            Refund refund = Refund.create(RefundId.generate(), "payment-id", AMOUNT, "reason");
            refund.process("EXT-REF-PROC");
            when(refundRepository.findById(any(RefundId.class))).thenReturn(Optional.of(refund));
            when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

            String refundMethod = "ORIGINAL_METHOD";
            Refund result = paymentService.completeRefund(refund.getId(), refundMethod);

            assertThat(result.getRefundMethod()).isEqualTo(refundMethod);

            ArgumentCaptor<RefundCompletedEvent> eventCaptor = ArgumentCaptor.forClass(RefundCompletedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            RefundCompletedEvent event = eventCaptor.getValue();
            assertThat(event.getRefundId()).isEqualTo(refund.getId());
            assertThat(event.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(event.getRefundMethod()).isEqualTo(refundMethod);
        }

        @Test
        @DisplayName("should throw when refund not found")
        void shouldThrowWhenRefundNotFound() {
            when(refundRepository.findById(any(RefundId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.completeRefund(RefundId.generate(), "method"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Refund not found");
        }
    }

    // =========================================================================
    // getPayment
    // =========================================================================

    @Nested
    @DisplayName("getPayment")
    class GetPayment {

        @Test
        @DisplayName("should return payment when found")
        void shouldReturnPaymentWhenFound() {
            Payment payment = createPendingPayment();
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.of(payment));

            Payment result = paymentService.getPayment(payment.getId());

            assertThat(result).isSameAs(payment);
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPayment(PaymentId.generate()))
                .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    // =========================================================================
    // getPaymentsByPatron
    // =========================================================================

    @Nested
    @DisplayName("getPaymentsByPatron")
    class GetPaymentsByPatron {

        @Test
        @DisplayName("should return list of payments for a patron")
        void shouldReturnListOfPaymentsForPatron() {
            Payment payment1 = Payment.create(PATRON_ID, PAYMENT_TYPE, AMOUNT, PAYMENT_METHOD, "First");
            Payment payment2 = Payment.create(PATRON_ID, PAYMENT_TYPE, new BigDecimal("200.00"), PaymentMethod.WECHAT_PAY, "Second");
            when(paymentRepository.findByPatronId(any(PatronId.class))).thenReturn(List.of(payment1, payment2));

            List<Payment> result = paymentService.getPaymentsByPatron(PATRON_ID);

            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(payment1, payment2);
        }

        @Test
        @DisplayName("should return empty list when patron has no payments")
        void shouldReturnEmptyListWhenNoPayments() {
            when(paymentRepository.findByPatronId(any(PatronId.class))).thenReturn(Collections.emptyList());

            List<Payment> result = paymentService.getPaymentsByPatron(PATRON_ID);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // getRefundsByPayment
    // =========================================================================

    @Nested
    @DisplayName("getRefundsByPayment")
    class GetRefundsByPayment {

        @Test
        @DisplayName("should return list of refunds for a payment")
        void shouldReturnListOfRefundsForPayment() {
            PaymentId paymentId = PaymentId.generate();
            Refund refund1 = Refund.create(RefundId.generate(), paymentId.getValue(), new BigDecimal("30.00"), "Partial 1");
            Refund refund2 = Refund.create(RefundId.generate(), paymentId.getValue(), new BigDecimal("70.00"), "Partial 2");
            when(refundRepository.findByPaymentId(paymentId.getValue())).thenReturn(List.of(refund1, refund2));

            List<Refund> result = paymentService.getRefundsByPayment(paymentId);

            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(refund1, refund2);
        }

        @Test
        @DisplayName("should return empty list when payment has no refunds")
        void shouldReturnEmptyListWhenNoRefunds() {
            PaymentId paymentId = PaymentId.generate();
            when(refundRepository.findByPaymentId(paymentId.getValue())).thenReturn(Collections.emptyList());

            List<Refund> result = paymentService.getRefundsByPayment(paymentId);

            assertThat(result).isEmpty();
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
}
