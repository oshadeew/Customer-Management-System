package com.customer.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResultDto {

    private int totalRows;
    private int successCount;
    private int updatedCount;
    private int failedCount;
    private List<String> errors = new ArrayList<>();
}
