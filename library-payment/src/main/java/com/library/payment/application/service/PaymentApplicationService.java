package com.library.payment.application.service;

import com.library.payment.application.command.*;
import com.library.payment.application.dto.PaymentDTO;
import com.library.payment.application.dto.RefundDTO;
import com.library.payment.domain.model.Payment;
import com.library.payment.domain.model.Refund;
import com.library.payment.domain.service.PaymentService;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.PaymentId;
import com.library.shared.domain.model.RefundId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PaymentApplicationService {

    private final PaymentService paymentService;

    public PaymentApplicationService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Transactional
    public PaymentDTO createPayment(CreatePaymentCommand command) {
        Payment payment = paymentService.createPayment(
            command.getPatronId(),
            command.getPaymentType(),
            command.getAmount(),
            command.getPaymentMethod(),
            command.getDescription()
        );
        return PaymentDTO.fromDomain(payment);
    }

    @Transactional
    public PaymentDTO processPayment(ProcessPaymentCommand command) {
        Payment payment = paymentService.processPayment(
            command.getPaymentId(),
            command.getExternalTransactionId()
        );
        return PaymentDTO.fromDomain(payment);
    }

    @Transactional
    public PaymentDTO completePayment(CompletePaymentCommand command) {
        Payment payment = paymentService.completePayment(command.getPaymentId());
        return PaymentDTO.fromDomain(payment);
    }

    @Transactional
    public PaymentDTO failPayment(FailPaymentCommand command) {
        Payment payment = paymentService.failPayment(
            command.getPaymentId(),
            command.getReason()
        );
        return PaymentDTO.fromDomain(payment);
    }

    @Transactional
    public PaymentDTO cancelPayment(CancelPaymentCommand command) {
        Payment payment = paymentService.cancelPayment(
            command.getPaymentId(),
            command.getReason()
        );
        return PaymentDTO.fromDomain(payment);
    }

    @Transactional
    public RefundDTO requestRefund(RequestRefundCommand command) {
        Refund refund = paymentService.requestRefund(
            command.getPaymentId(),
            command.getAmount(),
            command.getReason()
        );
        return RefundDTO.fromDomain(refund);
    }

    @Transactional
    public RefundDTO processRefund(ProcessRefundCommand command) {
        Refund refund = paymentService.processRefund(
            command.getRefundId(),
            command.getExternalRefundId()
        );
        return RefundDTO.fromDomain(refund);
    }

    @Transactional
    public RefundDTO completeRefund(CompleteRefundCommand command) {
        Refund refund = paymentService.completeRefund(
            command.getRefundId(),
            command.getRefundMethod()
        );
        return RefundDTO.fromDomain(refund);
    }

    public PaymentDTO getPayment(String id) {
        Payment payment = paymentService.getPayment(PaymentId.of(id));
        return PaymentDTO.fromDomain(payment);
    }

    public List<PaymentDTO> getPaymentsByPatron(String patronId) {
        return paymentService.getPaymentsByPatron(PatronId.of(patronId)).stream()
            .map(PaymentDTO::fromDomain)
            .collect(Collectors.toList());
    }

    public List<RefundDTO> getRefundsByPayment(String paymentId) {
        return paymentService.getRefundsByPayment(PaymentId.of(paymentId)).stream()
            .map(RefundDTO::fromDomain)
            .collect(Collectors.toList());
    }
}
