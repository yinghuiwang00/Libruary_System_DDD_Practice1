package com.library.inventory.application.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateInventoryCommand {
    @NotBlank(message = "Book ID must not be blank")
    private String bookId;

    @NotBlank(message = "Library ID must not be blank")
    private String libraryId;

    @Min(value = 0, message = "Initial copy count must be non-negative")
    private int initialCopyCount;

    private String libraryCode;
    private Integer floor;
    private String zone;
    private String aisle;
    private String shelf;
    private String position;
    private BigDecimal cost;
    private String createdBy;
}
