package com.library.payment.application.command;

import com.library.payment.domain.model.enums.PaymentMethod;
import com.library.payment.domain.model.enums.PaymentType;
import com.library.shared.domain.model.PatronId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class CreatePaymentCommand {

    @NotNull(message = "Patron ID must not be null")
    private PatronId patronId;

    @NotNull(message = "Payment type must not be null")
    private PaymentType paymentType;

    @NotNull(message = "Amount must not be null")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Payment method must not be null")
    private PaymentMethod paymentMethod;

    private String description;

    public CreatePaymentCommand() {
    }

    public CreatePaymentCommand(PatronId patronId, PaymentType paymentType,
                                BigDecimal amount, PaymentMethod paymentMethod,
                                String description) {
        this.patronId = patronId;
        this.paymentType = paymentType;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.description = description;
    }

    public PatronId getPatronId() { return patronId; }
    public void setPatronId(PatronId patronId) { this.patronId = patronId; }
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
