package com.customer.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
  Lightweight DTO used in paginated list responses.
 */
@Data
public class CustomerSummaryDto {

    private Long id;
    private String name;
    private String nicNumber;
    private LocalDate dateOfBirth;
    private LocalDateTime createdAt;
}
