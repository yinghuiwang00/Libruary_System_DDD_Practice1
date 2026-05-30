package com.library.payment.application.command;

import com.library.shared.domain.model.PaymentId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProcessPaymentCommand {

    @NotNull(message = "Payment ID must not be null")
    private PaymentId paymentId;

    @NotBlank(message = "External transaction ID must not be blank")
    private String externalTransactionId;

    public ProcessPaymentCommand() {
    }

    public ProcessPaymentCommand(PaymentId paymentId, String externalTransactionId) {
        this.paymentId = paymentId;
        this.externalTransactionId = externalTransactionId;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public void setPaymentId(PaymentId paymentId) { this.paymentId = paymentId; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public void setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; }
}
