package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.*;

import java.time.LocalDateTime;

public class HoldFulfilledEvent extends DomainEvent {

    private final HoldId holdId;
    private final BookId bookId;
    private final PatronId patronId;
    private final CopyId copyId;
    private final LocalDateTime availableUntil;
    private final LocalDateTime fulfilledAt;

    public HoldFulfilledEvent(HoldId holdId, BookId bookId, PatronId patronId,
                              CopyId copyId, LocalDateTime availableUntil, LocalDateTime fulfilledAt) {
        super();
        this.holdId = holdId;
        this.bookId = bookId;
        this.patronId = patronId;
        this.copyId = copyId;
        this.availableUntil = availableUntil;
        this.fulfilledAt = fulfilledAt;
    }

    public HoldId getHoldId() { return holdId; }
    public BookId getBookId() { return bookId; }
    public PatronId getPatronId() { return patronId; }
    public CopyId getCopyId() { return copyId; }
    public LocalDateTime getAvailableUntil() { return availableUntil; }
    public LocalDateTime getFulfilledAt() { return fulfilledAt; }
}
