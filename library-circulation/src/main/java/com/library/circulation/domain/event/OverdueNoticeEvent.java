package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.LoanId;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.CopyId;

public class OverdueNoticeEvent extends DomainEvent {

    private final LoanId loanId;
    private final PatronId patronId;
    private final CopyId copyId;
    private final long daysOverdue;

    public OverdueNoticeEvent(LoanId loanId, PatronId patronId, CopyId copyId, long daysOverdue) {
        super();
        this.loanId = loanId;
        this.patronId = patronId;
        this.copyId = copyId;
        this.daysOverdue = daysOverdue;
    }

    public LoanId getLoanId() { return loanId; }
    public PatronId getPatronId() { return patronId; }
    public CopyId getCopyId() { return copyId; }
    public long getDaysOverdue() { return daysOverdue; }
}
