package com.customer.dto;

import lombok.Data;

@Data
public class AddressResponseDto {
    private Long id;
    private String addressLine1;
    private String addressLine2;
    private Long cityId;
    private String cityName;
    private Long countryId;
    private String countryName;
}
