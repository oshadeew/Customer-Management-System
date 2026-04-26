package com.customer.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PhoneRequestDto {

    @NotBlank(message = "Phone number cannot be blank")
    private String phoneNumber;
}
