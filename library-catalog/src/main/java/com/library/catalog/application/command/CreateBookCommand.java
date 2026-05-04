package com.library.catalog.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBookCommand(
    @NotBlank @Size(max = 20) String isbn,
    @NotBlank @Size(max = 500) String title,
    @Size(max = 2000) String description,
    String publicationDate,
    Integer pageCount,
    String language
) {}
