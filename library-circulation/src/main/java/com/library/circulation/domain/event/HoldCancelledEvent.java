package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.BookId;
import com.library.shared.domain.model.HoldId;
import com.library.shared.domain.model.PatronId;

import java.time.LocalDateTime;

public class HoldCancelledEvent extends DomainEvent {

    private final HoldId holdId;
    private final BookId bookId;
    private final PatronId patronId;
    private final String reason;
    private final LocalDateTime cancelledAt;

    public HoldCancelledEvent(HoldId holdId, BookId bookId, PatronId patronId,
                              String reason, LocalDateTime cancelledAt) {
        super();
        this.holdId = holdId;
        this.bookId = bookId;
        this.patronId = patronId;
        this.reason = reason;
        this.cancelledAt = cancelledAt;
    }

    public HoldId getHoldId() { return holdId; }
    public BookId getBookId() { return bookId; }
    public PatronId getPatronId() { return patronId; }
    public String getReason() { return reason; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}
