package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.HoldId;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.BookId;

public class HoldExpiredEvent extends DomainEvent {

    private final HoldId holdId;
    private final PatronId patronId;
    private final BookId bookId;

    public HoldExpiredEvent(HoldId holdId, PatronId patronId, BookId bookId) {
        super();
        this.holdId = holdId;
        this.patronId = patronId;
        this.bookId = bookId;
    }

    public HoldId getHoldId() { return holdId; }
    public PatronId getPatronId() { return patronId; }
    public BookId getBookId() { return bookId; }
}
