package com.library.catalog.domain.event;

import com.library.shared.domain.event.DomainEvent;

public class BookDeletedEvent extends DomainEvent {
    private final String bookId;
    private final String title;

    public BookDeletedEvent(String bookId, String title) {
        super();
        this.bookId = bookId;
        this.title = title;
    }

    public String getBookId() { return bookId; }
    public String getTitle() { return title; }
}
