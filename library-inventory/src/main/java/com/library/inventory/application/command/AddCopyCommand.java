package com.library.inventory.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AddCopyCommand {
    @NotBlank(message = "Inventory ID must not be blank")
    private String inventoryId;

    private String libraryCode;
    private Integer floor;
    private String zone;
    private String aisle;
    private String shelf;
    private String position;
    private String acquisitionMethod;
    private BigDecimal cost;
}
