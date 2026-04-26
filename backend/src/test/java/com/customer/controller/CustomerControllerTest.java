package com.customer.controller;

import com.customer.dto.*;
import com.customer.service.BulkUploadService;
import com.customer.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private BulkUploadService bulkUploadService;

    private ObjectMapper objectMapper;
    private CustomerResponseDto sampleResponse;
    private CustomerRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        sampleResponse = new CustomerResponseDto();
        sampleResponse.setId(1L);
        sampleResponse.setName("Alice Fernando");
        sampleResponse.setNicNumber("901234567V");
        sampleResponse.setDateOfBirth(LocalDate.of(1990, 5, 15));
        sampleResponse.setCreatedAt(LocalDateTime.now());

        sampleRequest = new CustomerRequestDto();
        sampleRequest.setName("Alice Fernando");
        sampleRequest.setNicNumber("901234567V");
        sampleRequest.setDateOfBirth(LocalDate.of(1990, 5, 15));
    }

    // ----------------------------------------------------------------
    // POST /api/customers
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /api/customers - 201 Created with valid request")
    void createCustomer_success() throws Exception {
        when(customerService.create(any(CustomerRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice Fernando"))
                .andExpect(jsonPath("$.nicNumber").value("901234567V"));
    }

    @Test
    @DisplayName("POST /api/customers - 400 Bad Request when name is blank")
    void createCustomer_missingName_returns400() throws Exception {
        sampleRequest.setName(""); // blank name

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/customers - 400 Bad Request when NIC is blank")
    void createCustomer_missingNic_returns400() throws Exception {
        sampleRequest.setNicNumber("");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest());
    }

    // PUT /api/customers/{id}

    @Test
    @DisplayName("PUT /api/customers/{id} - 200 OK with valid request")
    void updateCustomer_success() throws Exception {
        sampleResponse.setName("Alice Updated");
        when(customerService.update(eq(1L), any(CustomerRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"));
    }

    // GET /api/customers/{id}

    @Test
    @DisplayName("GET /api/customers/{id} - 200 OK returns customer details")
    void getCustomerById_success() throws Exception {
        when(customerService.findById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nicNumber").value("901234567V"));
    }

    // GET /api/customers (paginated)

    @Test
    @DisplayName("GET /api/customers - 200 OK returns paginated list")
    void getCustomers_paginated_success() throws Exception {
        CustomerSummaryDto summary = new CustomerSummaryDto();
        summary.setId(1L);
        summary.setName("Alice Fernando");
        summary.setNicNumber("901234567V");
        summary.setDateOfBirth(LocalDate.of(1990, 5, 15));

        Page<CustomerSummaryDto> page = new PageImpl<>(
                Collections.singletonList(summary),
                PageRequest.of(0, 10),
                1
        );

        when(customerService.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/customers").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Alice Fernando"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // GET /api/customers/search

    @Test
    @DisplayName("GET /api/customers/search?name=alice - returns matching customers")
    void searchCustomers_success() throws Exception {
        CustomerSummaryDto summary = new CustomerSummaryDto();
        summary.setId(1L);
        summary.setName("Alice Fernando");
        summary.setNicNumber("901234567V");

        when(customerService.searchByName("alice")).thenReturn(Collections.singletonList(summary));

        mockMvc.perform(get("/api/customers/search").param("name", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice Fernando"));
    }

    // POST /api/customers/bulk-upload

    @Test
    @DisplayName("POST /api/customers/bulk-upload - 200 OK with valid file")
    void bulkUpload_success() throws Exception {
        BulkUploadResultDto resultDto = new BulkUploadResultDto();
        resultDto.setTotalRows(3);
        resultDto.setSuccessCount(2);
        resultDto.setUpdatedCount(1);
        resultDto.setFailedCount(0);

        when(bulkUploadService.processFile(any())).thenReturn(resultDto);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/customers/bulk-upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.updatedCount").value(1));
    }

    @Test
    @DisplayName("POST /api/customers/bulk-upload - 400 Bad Request when file is empty")
    void bulkUpload_emptyFile_returns400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/customers/bulk-upload").file(emptyFile))
                .andExpect(status().isBadRequest());
    }
}
