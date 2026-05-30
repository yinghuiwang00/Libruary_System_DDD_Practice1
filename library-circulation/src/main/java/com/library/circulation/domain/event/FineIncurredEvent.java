package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.FineId;
import com.library.shared.domain.model.LoanId;
import com.library.shared.domain.model.PatronId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FineIncurredEvent extends DomainEvent {

    private final FineId fineId;
    private final LoanId loanId;
    private final PatronId patronId;
    private final BigDecimal amount;
    private final int overdueDays;
    private final LocalDateTime incurredAt;

    public FineIncurredEvent(FineId fineId, LoanId loanId, PatronId patronId,
                             BigDecimal amount, int overdueDays, LocalDateTime incurredAt) {
        super();
        this.fineId = fineId;
        this.loanId = loanId;
        this.patronId = patronId;
        this.amount = amount;
        this.overdueDays = overdueDays;
        this.incurredAt = incurredAt;
    }

    public FineId getFineId() { return fineId; }
    public LoanId getLoanId() { return loanId; }
    public PatronId getPatronId() { return patronId; }
    public BigDecimal getAmount() { return amount; }
    public int getOverdueDays() { return overdueDays; }
    public LocalDateTime getIncurredAt() { return incurredAt; }
}
