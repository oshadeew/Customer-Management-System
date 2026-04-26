package com.customer.dto;

import lombok.Data;

/*
  Represents a family member reference inside customer detail response.
 */
@Data
public class FamilyMemberDto {
    private Long id;
    private String name;
    private String nicNumber;
}
