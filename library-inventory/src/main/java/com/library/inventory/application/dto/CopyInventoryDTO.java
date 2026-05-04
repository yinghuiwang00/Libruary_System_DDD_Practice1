package com.library.inventory.application.dto;

import com.library.inventory.domain.model.CopyInventory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class CopyInventoryDTO {
    private String id;
    private String bookId;
    private String libraryId;
    private String libraryCode;
    private Integer totalCopies;
    private Integer availableCopies;
    private Double availabilityRate;
    private Boolean lowStock;
    private List<BookCopyDTO> copies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CopyInventoryDTO fromDomain(CopyInventory inventory) {
        return CopyInventoryDTO.builder()
            .id(inventory.getId().getValue())
            .bookId(inventory.getBookId())
            .libraryId(inventory.getLibraryId())
            .libraryCode(inventory.getLibraryCode())
            .totalCopies(inventory.getTotalCopies())
            .availableCopies(inventory.getAvailableCopies())
            .availabilityRate(inventory.getAvailabilityRate())
            .lowStock(inventory.isLowStock())
            .copies(inventory.getCopies().stream()
                .map(BookCopyDTO::fromDomain)
                .collect(Collectors.toList()))
            .createdAt(inventory.getCreatedAt())
            .updatedAt(inventory.getUpdatedAt())
            .build();
    }
}
