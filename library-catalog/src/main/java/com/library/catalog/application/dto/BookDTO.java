package com.library.catalog.application.dto;

import com.library.catalog.domain.model.Book;
import com.library.catalog.domain.model.BookAuthor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BookDTO(
    String id,
    String isbn,
    String title,
    String description,
    LocalDate publicationDate,
    Integer pageCount,
    String language,
    String status,
    String publisherId,
    List<AuthorDTO> authors,
    List<String> categoryIds,
    Long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static BookDTO from(Book book) {
        return new BookDTO(
            book.getId().getValue(),
            book.getIsbn().getValue(),
            book.getTitle(),
            book.getDescription(),
            book.getPublicationDate(),
            book.getPageCount(),
            book.getLanguage(),
            book.getStatus().name(),
            book.getPublisherId(),
            book.getAuthors().stream().map(AuthorDTO::from).toList(),
            book.getCategoryIds(),
            book.getVersion(),
            book.getCreatedAt(),
            book.getUpdatedAt()
        );
    }
}
