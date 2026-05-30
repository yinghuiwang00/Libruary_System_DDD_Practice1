package com.library.payment.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.PaymentId;
import com.library.shared.domain.model.PatronId;

import java.math.BigDecimal;

public class PaymentCancelledEvent extends DomainEvent {

    private final PaymentId paymentId;
    private final PatronId patronId;
    private final BigDecimal amount;
    private final String reason;

    public PaymentCancelledEvent(PaymentId paymentId, PatronId patronId,
                                  BigDecimal amount, String reason) {
        super();
        this.paymentId = paymentId;
        this.patronId = patronId;
        this.amount = amount;
        this.reason = reason;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public PatronId getPatronId() { return patronId; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
}
