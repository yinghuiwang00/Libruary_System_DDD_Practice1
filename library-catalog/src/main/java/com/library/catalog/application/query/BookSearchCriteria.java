package com.library.catalog.application.query;

import com.library.catalog.domain.model.enums.BookStatus;

public record BookSearchCriteria(
    String title,
    String authorName,
    BookStatus status,
    String publisherId,
    String categoryId,
    String language
) {
    public boolean hasAnyFilter() {
        return (title != null && !title.isBlank())
            || (authorName != null && !authorName.isBlank())
            || status != null
            || (publisherId != null && !publisherId.isBlank())
            || (categoryId != null && !categoryId.isBlank())
            || (language != null && !language.isBlank());
    }
}
