package com.library.payment.application.command;

import com.library.shared.domain.model.PaymentId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FailPaymentCommand {

    @NotNull(message = "Payment ID must not be null")
    private PaymentId paymentId;

    @NotBlank(message = "Reason must not be blank")
    private String reason;

    public FailPaymentCommand() {
    }

    public FailPaymentCommand(PaymentId paymentId, String reason) {
        this.paymentId = paymentId;
        this.reason = reason;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public void setPaymentId(PaymentId paymentId) { this.paymentId = paymentId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
