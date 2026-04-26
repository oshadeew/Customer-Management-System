package com.customer.controller;

import com.customer.dto.BulkUploadResultDto;
import com.customer.dto.CustomerRequestDto;
import com.customer.dto.CustomerResponseDto;
import com.customer.dto.CustomerSummaryDto;
import com.customer.service.BulkUploadService;
import com.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerService customerService;
    private final BulkUploadService bulkUploadService;

    /*
      POST /api/customers - Create a new customer.
     */
    @PostMapping
    public ResponseEntity<CustomerResponseDto> create(@Valid @RequestBody CustomerRequestDto dto) {
        CustomerResponseDto response = customerService.create(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /*
      PUT /api/customers/{id} - Update an existing customer.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequestDto dto) {
        CustomerResponseDto response = customerService.update(id, dto);
        return ResponseEntity.ok(response);
    }

    /*
      GET /api/customers/{id} - Get a single customer with all details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    /*
      GET /api/customers?page=0&size=10 - Get paginated customer list.
     */
    @GetMapping
    public ResponseEntity<Page<CustomerSummaryDto>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(customerService.findAll(pageable));
    }

    /*
      GET /api/customers/search?name=xyz - Search customers by name (for family member selector).
     */
    @GetMapping("/search")
    public ResponseEntity<List<CustomerSummaryDto>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(customerService.searchByName(name));
    }

    /*
      POST /api/customers/bulk-upload - Upload Excel file for bulk create/update.
     */
    @PostMapping("/bulk-upload")
    public ResponseEntity<BulkUploadResultDto> bulkUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            BulkUploadResultDto result = bulkUploadService.processFile(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            BulkUploadResultDto errorResult = new BulkUploadResultDto();
            errorResult.getErrors().add("Failed to process file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
}
