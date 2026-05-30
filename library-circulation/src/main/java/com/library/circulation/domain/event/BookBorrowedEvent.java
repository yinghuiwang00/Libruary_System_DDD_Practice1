package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.BookId;
import com.library.shared.domain.model.CopyId;
import com.library.shared.domain.model.LoanId;
import com.library.shared.domain.model.PatronId;

import java.time.LocalDateTime;

public class BookBorrowedEvent extends DomainEvent {

    private final LoanId loanId;
    private final CopyId copyId;
    private final PatronId patronId;
    private final BookId bookId;
    private final LocalDateTime loanDate;
    private final LocalDateTime dueDate;

    public BookBorrowedEvent(LoanId loanId, CopyId copyId, PatronId patronId,
                             BookId bookId, LocalDateTime loanDate, LocalDateTime dueDate) {
        super();
        this.loanId = loanId;
        this.copyId = copyId;
        this.patronId = patronId;
        this.bookId = bookId;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
    }

    public LoanId getLoanId() { return loanId; }
    public CopyId getCopyId() { return copyId; }
    public PatronId getPatronId() { return patronId; }
    public BookId getBookId() { return bookId; }
    public LocalDateTime getLoanDate() { return loanDate; }
    public LocalDateTime getDueDate() { return dueDate; }
}
