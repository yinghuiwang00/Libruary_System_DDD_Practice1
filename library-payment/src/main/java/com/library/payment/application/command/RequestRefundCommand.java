package com.library.payment.application.command;

import com.library.shared.domain.model.PaymentId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class RequestRefundCommand {

    @NotNull(message = "Payment ID must not be null")
    private PaymentId paymentId;

    @NotNull(message = "Amount must not be null")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String reason;

    public RequestRefundCommand() {
    }

    public RequestRefundCommand(PaymentId paymentId, BigDecimal amount, String reason) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public void setPaymentId(PaymentId paymentId) { this.paymentId = paymentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
