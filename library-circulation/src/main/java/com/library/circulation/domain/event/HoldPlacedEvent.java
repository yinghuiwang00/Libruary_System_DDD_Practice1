package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.BookId;
import com.library.shared.domain.model.HoldId;
import com.library.shared.domain.model.PatronId;

import java.time.LocalDateTime;

public class HoldPlacedEvent extends DomainEvent {

    private final HoldId holdId;
    private final BookId bookId;
    private final PatronId patronId;
    private final int queuePosition;
    private final LocalDateTime placedAt;

    public HoldPlacedEvent(HoldId holdId, BookId bookId, PatronId patronId,
                           int queuePosition, LocalDateTime placedAt) {
        super();
        this.holdId = holdId;
        this.bookId = bookId;
        this.patronId = patronId;
        this.queuePosition = queuePosition;
        this.placedAt = placedAt;
    }

    public HoldId getHoldId() { return holdId; }
    public BookId getBookId() { return bookId; }
    public PatronId getPatronId() { return patronId; }
    public int getQueuePosition() { return queuePosition; }
    public LocalDateTime getPlacedAt() { return placedAt; }
}
