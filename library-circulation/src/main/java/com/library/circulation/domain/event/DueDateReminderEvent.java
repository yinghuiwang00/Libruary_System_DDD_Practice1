package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.LoanId;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.CopyId;

import java.time.LocalDateTime;

public class DueDateReminderEvent extends DomainEvent {

    private final LoanId loanId;
    private final PatronId patronId;
    private final CopyId copyId;
    private final LocalDateTime dueDate;

    public DueDateReminderEvent(LoanId loanId, PatronId patronId, CopyId copyId, LocalDateTime dueDate) {
        super();
        this.loanId = loanId;
        this.patronId = patronId;
        this.copyId = copyId;
        this.dueDate = dueDate;
    }

    public LoanId getLoanId() { return loanId; }
    public PatronId getPatronId() { return patronId; }
    public CopyId getCopyId() { return copyId; }
    public LocalDateTime getDueDate() { return dueDate; }
}
