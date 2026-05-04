package com.library.inventory.application.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BatchAddCopiesCommand {
    @NotBlank(message = "Inventory ID must not be blank")
    private String inventoryId;

    @Min(value = 1, message = "Count must be at least 1")
    private int count;

    private String libraryCode;
    private Integer floor;
    private String zone;
    private String aisle;
    private String shelf;
    private String position;
    private String acquisitionMethod;
    private BigDecimal cost;
}
