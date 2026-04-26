package com.customer.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AddressRequestDto {

    @NotBlank(message = "Address line 1 is mandatory")
    private String addressLine1;

    private String addressLine2;

    @NotNull(message = "City ID is mandatory")
    private Long cityId;
}
