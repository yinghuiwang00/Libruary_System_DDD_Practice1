package com.library.payment.domain.service;

import com.library.payment.domain.event.*;
import com.library.payment.domain.exception.InvalidOperationException;
import com.library.payment.domain.exception.PaymentNotFoundException;
import com.library.payment.domain.model.Payment;
import com.library.payment.domain.model.Refund;
import com.library.payment.domain.model.enums.PaymentMethod;
import com.library.payment.domain.model.enums.PaymentType;
import com.library.payment.domain.repository.PaymentRepository;
import com.library.payment.domain.repository.RefundRepository;
import com.library.shared.domain.event.DomainEventPublisher;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.PaymentId;
import com.library.shared.domain.model.RefundId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final DomainEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          RefundRepository refundRepository,
                          DomainEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Payment createPayment(PatronId patronId, PaymentType paymentType,
                                  BigDecimal amount, PaymentMethod paymentMethod,
                                  String description) {
        Payment payment = Payment.create(patronId, paymentType, amount, paymentMethod, description);
        Payment saved = paymentRepository.save(payment);

        eventPublisher.publish(new PaymentCreatedEvent(
            saved.getId(), saved.getPatronId(), saved.getAmount(), saved.getPaymentMethod()
        ));
        return saved;
    }

    @Transactional
    public Payment processPayment(PaymentId paymentId, String externalTransactionId) {
        Payment payment = findPaymentOrThrow(paymentId);
        payment.process(externalTransactionId);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment completePayment(PaymentId paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        payment.complete();
        Payment saved = paymentRepository.save(payment);

        eventPublisher.publish(new PaymentCompletedEvent(
            saved.getId(), saved.getPatronId(), saved.getAmount(),
            saved.getReferenceNumber(), saved.getPaymentDate()
        ));
        return saved;
    }

    @Transactional
    public Payment failPayment(PaymentId paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        payment.fail(reason);
        Payment saved = paymentRepository.save(payment);

        eventPublisher.publish(new PaymentFailedEvent(
            saved.getId(), saved.getPatronId(), saved.getAmount(), reason
        ));
        return saved;
    }

    @Transactional
    public Payment cancelPayment(PaymentId paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        payment.cancel(reason);
        Payment saved = paymentRepository.save(payment);

        eventPublisher.publish(new PaymentCancelledEvent(
            saved.getId(), saved.getPatronId(), saved.getAmount(), reason
        ));
        return saved;
    }

    @Transactional
    public Refund requestRefund(PaymentId paymentId, BigDecimal amount, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        RefundId refundId = RefundId.generate();
        Refund refund = payment.requestRefund(refundId, amount, reason);

        // Load existing refunds from persistence for accurate calculation
        List<Refund> existingRefunds = refundRepository.findByPaymentId(paymentId.getValue());
        for (Refund existing : existingRefunds) {
            if (!payment.getRefunds().contains(existing)) {
                payment.getRefunds().add(existing);
            }
        }

        // Re-check refundable amount with all persisted refunds
        BigDecimal totalRefunded = BigDecimal.ZERO;
        for (Refund r : payment.getRefunds()) {
            if (r.isCompleted()) {
                totalRefunded = totalRefunded.add(r.getAmount());
            }
        }
        BigDecimal refundable = payment.getAmount().subtract(totalRefunded);
        if (amount.compareTo(refundable) > 0) {
            throw new InvalidOperationException(
                "Refund amount " + amount + " exceeds refundable amount " + refundable);
        }

        Refund savedRefund = refundRepository.save(refund);
        paymentRepository.save(payment);

        eventPublisher.publish(new RefundRequestedEvent(
            savedRefund.getId(), paymentId, savedRefund.getAmount(), reason
        ));
        return savedRefund;
    }

    @Transactional
    public Refund processRefund(RefundId refundId, String externalRefundId) {
        Refund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> new InvalidOperationException("Refund not found: " + refundId));
        refund.process(externalRefundId);
        return refundRepository.save(refund);
    }

    @Transactional
    public Refund completeRefund(RefundId refundId, String refundMethod) {
        Refund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> new InvalidOperationException("Refund not found: " + refundId));
        refund.complete(refundMethod);
        Refund saved = refundRepository.save(refund);

        eventPublisher.publish(new RefundCompletedEvent(
            saved.getId(), PaymentId.of(saved.getPaymentId()),
            saved.getAmount(), saved.getRefundMethod()
        ));
        return saved;
    }

    public Payment getPayment(PaymentId paymentId) {
        return findPaymentOrThrow(paymentId);
    }

    public List<Payment> getPaymentsByPatron(PatronId patronId) {
        return paymentRepository.findByPatronId(patronId);
    }

    public List<Refund> getRefundsByPayment(PaymentId paymentId) {
        return refundRepository.findByPaymentId(paymentId.getValue());
    }

    private Payment findPaymentOrThrow(PaymentId paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
