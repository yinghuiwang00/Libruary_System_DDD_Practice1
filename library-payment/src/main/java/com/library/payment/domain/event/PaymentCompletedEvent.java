package com.library.payment.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.PaymentId;
import com.library.shared.domain.model.PatronId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentCompletedEvent extends DomainEvent {

    private final PaymentId paymentId;
    private final PatronId patronId;
    private final BigDecimal amount;
    private final String referenceNumber;
    private final LocalDateTime paymentDate;

    public PaymentCompletedEvent(PaymentId paymentId, PatronId patronId,
                                  BigDecimal amount, String referenceNumber,
                                  LocalDateTime paymentDate) {
        super();
        this.paymentId = paymentId;
        this.patronId = patronId;
        this.amount = amount;
        this.referenceNumber = referenceNumber;
        this.paymentDate = paymentDate;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public PatronId getPatronId() { return patronId; }
    public BigDecimal getAmount() { return amount; }
    public String getReferenceNumber() { return referenceNumber; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
}
