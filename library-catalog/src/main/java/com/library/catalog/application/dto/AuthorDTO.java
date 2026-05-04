package com.library.catalog.application.dto;

import com.library.catalog.domain.model.Author;
import com.library.catalog.domain.model.BookAuthor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AuthorDTO(
    String id,
    String name,
    String biography,
    LocalDate birthDate,
    LocalDate deathDate,
    String nationality,
    String role,
    Long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AuthorDTO from(Author author) {
        return new AuthorDTO(
            author.getId().getValue(),
            author.getName(),
            author.getBiography(),
            author.getBirthDate(),
            author.getDeathDate(),
            author.getNationality(),
            null,
            author.getVersion(),
            author.getCreatedAt(),
            author.getUpdatedAt()
        );
    }

    public static AuthorDTO from(BookAuthor bookAuthor) {
        return new AuthorDTO(
            bookAuthor.getAuthorId(),
            bookAuthor.getAuthorName(),
            null,
            null,
            null,
            null,
            bookAuthor.getRole().name(),
            null,
            null,
            null
        );
    }
}
