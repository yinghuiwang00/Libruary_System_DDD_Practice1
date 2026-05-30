package com.library.circulation.domain.event;

import com.library.shared.domain.event.DomainEvent;
import com.library.shared.domain.model.HoldId;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.BookId;
import com.library.shared.domain.model.CopyId;

public class HoldPickedUpEvent extends DomainEvent {

    private final HoldId holdId;
    private final PatronId patronId;
    private final BookId bookId;
    private final CopyId copyId;

    public HoldPickedUpEvent(HoldId holdId, PatronId patronId, BookId bookId, CopyId copyId) {
        super();
        this.holdId = holdId;
        this.patronId = patronId;
        this.bookId = bookId;
        this.copyId = copyId;
    }

    public HoldId getHoldId() { return holdId; }
    public PatronId getPatronId() { return patronId; }
    public BookId getBookId() { return bookId; }
    public CopyId getCopyId() { return copyId; }
}
