package com.library.catalog.application.dto;

import com.library.catalog.domain.model.Publisher;

import java.time.LocalDateTime;

public record PublisherDTO(
    String id,
    String name,
    String description,
    String address,
    String phone,
    String email,
    String website,
    Long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static PublisherDTO from(Publisher publisher) {
        return new PublisherDTO(
            publisher.getId().getValue(),
            publisher.getName(),
            publisher.getDescription(),
            publisher.getAddress(),
            publisher.getPhone(),
            publisher.getEmail(),
            publisher.getWebsite(),
            publisher.getVersion(),
            publisher.getCreatedAt(),
            publisher.getUpdatedAt()
        );
    }
}
