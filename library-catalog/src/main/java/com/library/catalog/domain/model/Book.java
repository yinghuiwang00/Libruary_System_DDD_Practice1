package com.library.catalog.domain.model;

import com.library.catalog.domain.model.enums.BookStatus;
import com.library.catalog.domain.exception.*;
import com.library.shared.domain.model.AuthorId;
import com.library.shared.domain.model.BookId;
import com.library.shared.domain.model.CategoryId;
import com.library.shared.domain.model.PublisherId;
import com.library.catalog.domain.model.enums.AuthorRole;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "books")
public class Book {

    @EmbeddedId
    private BookId id;

    @Embedded
    private ISBN isbn;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(length = 50)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status;

    @Column(name = "publisher_id", length = 36)
    private String publisherId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id")
    private List<BookAuthor> authors = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "book_categories", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "category_id")
    private List<String> categoryIds = new ArrayList<>();

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    protected Book() {
    }

    private Book(BookId id, ISBN isbn, String title, String description,
                 LocalDate publicationDate, Integer pageCount, String language) {
        this.id = id;
        this.isbn = isbn;
        setTitle(title);
        this.description = description;
        this.publicationDate = publicationDate;
        this.pageCount = pageCount;
        this.language = language;
        this.status = BookStatus.DRAFT;
    }

    public static Book create(ISBN isbn, String title, String description,
                              LocalDate publicationDate, Integer pageCount, String language) {
        return new Book(BookId.generate(), isbn, title, description, publicationDate, pageCount, language);
    }

    public void addAuthor(String authorId, String authorName, AuthorRole role) {
        Objects.requireNonNull(authorId, "Author ID must not be null");
        boolean exists = authors.stream()
            .anyMatch(ba -> ba.getAuthorId().equals(authorId));
        if (exists) {
            throw new DuplicateAuthorException("Author already added to this book: " + authorId);
        }
        int sortOrder = authors.size();
        BookAuthor bookAuthor = BookAuthor.of(this.id,
            AuthorId.of(authorId), authorName, role, sortOrder);
        authors.add(bookAuthor);
    }

    public void removeAuthor(String authorId) {
        boolean removed = authors.removeIf(ba -> ba.getAuthorId().equals(authorId));
        if (!removed) {
            throw new AuthorNotFoundException("Author not found in this book: " + authorId);
        }
        reorderAuthors();
    }

    public void setPublisher(String publisherId) {
        this.publisherId = Objects.requireNonNull(publisherId, "Publisher ID must not be null");
    }

    public void removePublisher() {
        if (status == BookStatus.PUBLISHED) {
            throw new InvalidOperationException("Cannot remove publisher from published book");
        }
        this.publisherId = null;
    }

    public void addCategory(String categoryId) {
        Objects.requireNonNull(categoryId, "Category ID must not be null");
        if (categoryIds.contains(categoryId)) {
            throw new InvalidOperationException("Category already added: " + categoryId);
        }
        categoryIds.add(categoryId);
    }

    public void removeCategory(String categoryId) {
        boolean removed = categoryIds.remove(categoryId);
        if (!removed) {
            throw new CategoryNotFoundException("Category not found: " + categoryId);
        }
    }

    public void updateBasicInfo(String title, String description, LocalDate publicationDate,
                                Integer pageCount, String language) {
        if (status == BookStatus.DELETED) {
            throw new InvalidOperationException("Cannot update a deleted book");
        }
        if (title != null) setTitle(title);
        if (description != null) this.description = description;
        if (publicationDate != null) this.publicationDate = publicationDate;
        if (pageCount != null) this.pageCount = pageCount;
        if (language != null) this.language = language;
    }

    public void publish() {
        if (status != BookStatus.DRAFT && status != BookStatus.UNPUBLISHED) {
            throw new InvalidOperationException("Can only publish DRAFT or UNPUBLISHED books, current: " + status);
        }
        if (authors.isEmpty()) {
            throw new InvalidOperationException("Published book must have at least one author");
        }
        if (publisherId == null) {
            throw new InvalidOperationException("Published book must have a publisher");
        }
        if (categoryIds.isEmpty()) {
            throw new InvalidOperationException("Published book must have at least one category");
        }
        this.status = BookStatus.PUBLISHED;
    }

    public void unpublish() {
        if (status != BookStatus.PUBLISHED) {
            throw new InvalidOperationException("Can only unpublish PUBLISHED books, current: " + status);
        }
        this.status = BookStatus.UNPUBLISHED;
    }

    public void delete() {
        if (status == BookStatus.PUBLISHED) {
            throw new InvalidOperationException("Cannot delete a published book. Unpublish first.");
        }
        this.status = BookStatus.DELETED;
    }

    private void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Book title must not be blank");
        }
        if (title.length() > 500) {
            throw new IllegalArgumentException("Book title must not exceed 500 characters");
        }
        this.title = title;
    }

    private void reorderAuthors() {
        for (int i = 0; i < authors.size(); i++) {
            BookAuthor ba = authors.get(i);
            authors.set(i, BookAuthor.of(
                BookId.of(ba.getBookId()), AuthorId.of(ba.getAuthorId()),
                ba.getAuthorName(), ba.getRole(), i));
        }
    }

    public BookId getId() { return id; }
    public ISBN getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getPublicationDate() { return publicationDate; }
    public Integer getPageCount() { return pageCount; }
    public String getLanguage() { return language; }
    public BookStatus getStatus() { return status; }
    public String getPublisherId() { return publisherId; }
    public List<BookAuthor> getAuthors() { return Collections.unmodifiableList(authors); }
    public List<String> getCategoryIds() { return Collections.unmodifiableList(categoryIds); }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
