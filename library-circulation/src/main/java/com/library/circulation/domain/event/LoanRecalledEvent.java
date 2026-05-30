package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.LoanId;

import java.time.LocalDateTime;

public class LoanRecalledEvent extends DomainEvent {

    private final LoanId loanId;
    private final LocalDateTime newDueDate;
    private final String reason;
    private final LocalDateTime recalledAt;

    public LoanRecalledEvent(LoanId loanId, LocalDateTime newDueDate,
                             String reason, LocalDateTime recalledAt) {
        super();
        this.loanId = loanId;
        this.newDueDate = newDueDate;
        this.reason = reason;
        this.recalledAt = recalledAt;
    }

    public LoanId getLoanId() { return loanId; }
    public LocalDateTime getNewDueDate() { return newDueDate; }
    public String getReason() { return reason; }
    public LocalDateTime getRecalledAt() { return recalledAt; }
}
