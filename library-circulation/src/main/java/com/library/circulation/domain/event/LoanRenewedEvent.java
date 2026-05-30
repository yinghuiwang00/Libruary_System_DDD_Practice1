package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.LoanId;

import java.time.LocalDateTime;

public class LoanRenewedEvent extends DomainEvent {

    private final LoanId loanId;
    private final LocalDateTime oldDueDate;
    private final LocalDateTime newDueDate;
    private final int renewalCount;
    private final LocalDateTime renewedAt;

    public LoanRenewedEvent(LoanId loanId, LocalDateTime oldDueDate,
                            LocalDateTime newDueDate, int renewalCount, LocalDateTime renewedAt) {
        super();
        this.loanId = loanId;
        this.oldDueDate = oldDueDate;
        this.newDueDate = newDueDate;
        this.renewalCount = renewalCount;
        this.renewedAt = renewedAt;
    }

    public LoanId getLoanId() { return loanId; }
    public LocalDateTime getOldDueDate() { return oldDueDate; }
    public LocalDateTime getNewDueDate() { return newDueDate; }
    public int getRenewalCount() { return renewalCount; }
    public LocalDateTime getRenewedAt() { return renewedAt; }
}
