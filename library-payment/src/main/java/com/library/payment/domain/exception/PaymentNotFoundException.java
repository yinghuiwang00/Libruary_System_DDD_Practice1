package com.library.payment.domain.exception;

import com.library.shared.domain.model.PaymentId;

public class PaymentNotFoundException extends DomainException {

    public PaymentNotFoundException(PaymentId paymentId) {
        super("PAYMENT_NOT_FOUND", "Payment not found: " + paymentId);
    }
}
