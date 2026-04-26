package com.customer.dto;

import lombok.Data;

@Data
public class CityDto {
    private Long id;
    private String name;
    private Long countryId;
    private String countryName;
}
