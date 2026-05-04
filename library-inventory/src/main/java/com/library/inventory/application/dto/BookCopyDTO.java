package com.library.inventory.application.dto;

import com.library.inventory.domain.model.BookCopy;
import com.library.inventory.domain.model.Location;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BookCopyDTO {
    private String id;
    private String inventoryId;
    private String barcode;
    private String locationCode;
    private String locationDescription;
    private String status;
    private LocalDate acquisitionDate;
    private String acquisitionMethod;
    private BigDecimal acquisitionCost;
    private String condition;
    private LocalDate lastBorrowedDate;
    private Integer borrowCount;
    private String damageDescription;
    private String lostReason;
    private String removalReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookCopyDTO fromDomain(BookCopy copy) {
        Location loc = copy.getLocation();
        return BookCopyDTO.builder()
            .id(copy.getId().getValue())
            .inventoryId(copy.getInventoryId())
            .barcode(copy.getBarcode())
            .locationCode(loc != null ? loc.getLocationCode() : null)
            .locationDescription(loc != null ? loc.getFullDescription() : null)
            .status(copy.getStatus().name())
            .acquisitionDate(copy.getAcquisitionDate())
            .acquisitionMethod(copy.getAcquisitionMethod())
            .acquisitionCost(copy.getAcquisitionCost())
            .condition(copy.getCondition() != null ? copy.getCondition().name() : null)
            .lastBorrowedDate(copy.getLastBorrowedDate())
            .borrowCount(copy.getBorrowCount())
            .damageDescription(copy.getDamageDescription())
            .createdAt(copy.getCreatedAt())
            .updatedAt(copy.getUpdatedAt())
            .build();
    }
}
