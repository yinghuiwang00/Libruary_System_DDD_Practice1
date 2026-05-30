package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookReturnedEvent extends DomainEvent {

    private final LoanId loanId;
    private final CopyId copyId;
    private final PatronId patronId;
    private final BookId bookId;
    private final LocalDateTime returnDate;
    private final BigDecimal fineAmount;

    public BookReturnedEvent(LoanId loanId, CopyId copyId, PatronId patronId,
                             BookId bookId, LocalDateTime returnDate, BigDecimal fineAmount) {
        super();
        this.loanId = loanId;
        this.copyId = copyId;
        this.patronId = patronId;
        this.bookId = bookId;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
    }

    public LoanId getLoanId() { return loanId; }
    public CopyId getCopyId() { return copyId; }
    public PatronId getPatronId() { return patronId; }
    public BookId getBookId() { return bookId; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public BigDecimal getFineAmount() { return fineAmount; }
}
