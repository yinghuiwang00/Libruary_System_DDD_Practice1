package com.library.catalog.application.command;

import jakarta.validation.constraints.Size;

public record UpdateBookCommand(
    @Size(max = 500) String title,
    @Size(max = 2000) String description,
    String publicationDate,
    Integer pageCount,
    String language
) {}
