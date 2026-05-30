package com.library.catalog.domain.event;

import com.library.shared.domain.event.DomainEvent;

public class BookCreatedEvent extends DomainEvent {
    private final String bookId;
    private final String isbn;
    private final String title;

    public BookCreatedEvent(String bookId, String isbn, String title) {
        super();
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
    }

    public String getBookId() { return bookId; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
}
