package com.customer.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class CustomerRequestDto {

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotNull(message = "Date of birth is mandatory")
    private LocalDate dateOfBirth;

    @NotBlank(message = "NIC number is mandatory")
    private String nicNumber;

    @Valid
    private List<PhoneRequestDto> phones = new ArrayList<>();

    @Valid
    private List<AddressRequestDto> addresses = new ArrayList<>();

    private Set<Long> familyMemberIds = new HashSet<>();
}
