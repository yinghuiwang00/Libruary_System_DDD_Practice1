package com.library.catalog.domain.model;

import com.library.shared.domain.model.AuthorId;
import com.library.shared.domain.model.BookId;
import com.library.catalog.domain.model.enums.AuthorRole;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "book_authors")
@IdClass(BookAuthor.BookAuthorId.class)
public class BookAuthor {

    @Id
    @Column(name = "book_id", nullable = false)
    private String bookId;

    @Id
    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(name = "author_name", nullable = false, length = 200)
    private String authorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private AuthorRole role;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected BookAuthor() {
    }

    private BookAuthor(String bookId, String authorId, String authorName, AuthorRole role, int sortOrder) {
        this.bookId = Objects.requireNonNull(bookId);
        this.authorId = Objects.requireNonNull(authorId);
        this.authorName = Objects.requireNonNull(authorName);
        this.role = Objects.requireNonNull(role);
        this.sortOrder = sortOrder;
    }

    public static BookAuthor of(BookId bookId, AuthorId authorId, String authorName, AuthorRole role, int sortOrder) {
        return new BookAuthor(bookId.getValue(), authorId.getValue(), authorName, role, sortOrder);
    }

    public String getBookId() { return bookId; }
    public String getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public AuthorRole getRole() { return role; }
    public int getSortOrder() { return sortOrder; }

    public static class BookAuthorId implements Serializable {
        private String bookId;
        private String authorId;

        public BookAuthorId() {}

        public BookAuthorId(String bookId, String authorId) {
            this.bookId = bookId;
            this.authorId = authorId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BookAuthorId that = (BookAuthorId) o;
            return Objects.equals(bookId, that.bookId) && Objects.equals(authorId, that.authorId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bookId, authorId);
        }
    }
}
