package com.customer.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CustomerResponseDto {

    private Long id;
    private String name;
    private LocalDate dateOfBirth;
    private String nicNumber;
    private LocalDateTime createdAt;
    private List<PhoneResponseDto> phones = new ArrayList<>();
    private List<AddressResponseDto> addresses = new ArrayList<>();
    private List<FamilyMemberDto> familyMembers = new ArrayList<>();
}
