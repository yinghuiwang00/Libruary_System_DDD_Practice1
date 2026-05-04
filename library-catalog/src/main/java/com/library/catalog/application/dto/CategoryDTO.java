package com.library.catalog.application.dto;

import com.library.catalog.domain.model.Category;

import java.time.LocalDateTime;
import java.util.List;

public record CategoryDTO(
    String id,
    String name,
    String description,
    int level,
    String parentId,
    List<CategoryDTO> children,
    Long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CategoryDTO from(Category category) {
        return new CategoryDTO(
            category.getId().getValue(),
            category.getName(),
            category.getDescription(),
            category.getLevel(),
            category.getParent() != null ? category.getParent().getId().getValue() : null,
            category.getChildren().stream().map(CategoryDTO::from).toList(),
            category.getVersion(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
