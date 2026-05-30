package com.library.payment.application.command;

import com.library.shared.domain.model.PaymentId;
import jakarta.validation.constraints.NotNull;

public class CompletePaymentCommand {

    @NotNull(message = "Payment ID must not be null")
    private PaymentId paymentId;

    public CompletePaymentCommand() {
    }

    public CompletePaymentCommand(PaymentId paymentId) {
        this.paymentId = paymentId;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public void setPaymentId(PaymentId paymentId) { this.paymentId = paymentId; }
}
