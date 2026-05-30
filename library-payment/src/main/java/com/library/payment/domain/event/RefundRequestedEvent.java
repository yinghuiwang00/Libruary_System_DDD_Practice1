package com.library.payment.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.PaymentId;
import com.library.shared.domain.model.RefundId;

import java.math.BigDecimal;

public class RefundRequestedEvent extends DomainEvent {

    private final RefundId refundId;
    private final PaymentId paymentId;
    private final BigDecimal amount;
    private final String reason;

    public RefundRequestedEvent(RefundId refundId, PaymentId paymentId,
                                 BigDecimal amount, String reason) {
        super();
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
    }

    public RefundId getRefundId() { return refundId; }
    public PaymentId getPaymentId() { return paymentId; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
}
