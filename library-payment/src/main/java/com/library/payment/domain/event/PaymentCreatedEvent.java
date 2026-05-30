package com.library.payment.domain.event;

import com.library.payment.domain.model.enums.PaymentMethod;
import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.PaymentId;
import com.library.shared.domain.model.PatronId;

import java.math.BigDecimal;

public class PaymentCreatedEvent extends DomainEvent {

    private final PaymentId paymentId;
    private final PatronId patronId;
    private final BigDecimal amount;
    private final PaymentMethod paymentMethod;

    public PaymentCreatedEvent(PaymentId paymentId, PatronId patronId,
                                BigDecimal amount, PaymentMethod paymentMethod) {
        super();
        this.paymentId = paymentId;
        this.patronId = patronId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public PatronId getPatronId() { return patronId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
}
