package com.library.inventory.application.dto;

import com.library.inventory.domain.model.Library;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LibraryDTO {
    private String id;
    private String code;
    private String name;
    private String address;
    private String city;
    private String province;
    private String postalCode;
    private String phone;
    private String email;
    private String openingHours;
    private Integer totalFloors;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LibraryDTO fromDomain(Library library) {
        return LibraryDTO.builder()
            .id(library.getId().getValue())
            .code(library.getCode())
            .name(library.getName())
            .address(library.getAddress())
            .city(library.getCity())
            .province(library.getProvince())
            .postalCode(library.getPostalCode())
            .phone(library.getPhone())
            .email(library.getEmail())
            .openingHours(library.getOpeningHours())
            .totalFloors(library.getTotalFloors())
            .active(library.isActive())
            .createdAt(library.getCreatedAt())
            .updatedAt(library.getUpdatedAt())
            .build();
    }
}
