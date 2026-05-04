package com.library.inventory.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateLibraryCommand {
    @NotBlank(message = "Library code must not be blank")
    @Size(max = 20, message = "Library code must not exceed 20 characters")
    private String code;

    @NotBlank(message = "Library name must not be blank")
    @Size(max = 200, message = "Library name must not exceed 200 characters")
    private String name;

    private String address;
    private String city;
    private String province;
    private String postalCode;
    private String phone;
    private String email;
    private String openingHours;
    private Integer totalFloors;
}
