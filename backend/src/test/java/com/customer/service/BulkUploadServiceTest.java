package com.customer.service;

import com.customer.dto.BulkUploadResultDto;
import com.customer.entity.Customer;
import com.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkUploadServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private BulkUploadService bulkUploadService;

    @BeforeEach
    void setUp() {
        // Wire the self-reference manually (Spring would do this via @Autowired setter)
        bulkUploadService.setSelf(bulkUploadService);
    }

    // processBatch – INSERT tests

    @Test
    @DisplayName("processBatch() - inserts new customers when NIC does not exist")
    void processBatch_insertsNewCustomers() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Alice Fernando", "1990-05-15", "NIC001"},
                new String[]{"Bob Perera",     "1985-11-20", "NIC002"}
        );

        when(customerRepository.findByNicNumberIn(anyList())).thenReturn(Collections.emptyList());
        when(customerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        BulkUploadResultDto result = new BulkUploadResultDto();
        bulkUploadService.processBatch(rows, 2, result);

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getUpdatedCount()).isEqualTo(0);
        assertThat(result.getFailedCount()).isEqualTo(0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Customer>> captor = ArgumentCaptor.forClass(List.class);
        verify(customerRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getNicNumber()).isEqualTo("NIC001");
    }

    // processBatch – UPDATE tests

    @Test
    @DisplayName("processBatch() - updates existing customers when NIC already exists")
    void processBatch_updatesExistingCustomers() {
        Customer existingCustomer = new Customer();
        existingCustomer.setId(1L);
        existingCustomer.setName("Old Name");
        existingCustomer.setDateOfBirth(LocalDate.of(1980, 1, 1));
        existingCustomer.setNicNumber("NIC001");
        existingCustomer.setPhones(new ArrayList<>());
        existingCustomer.setAddresses(new ArrayList<>());
        existingCustomer.setFamilyMembers(new HashSet<>());

        List<String[]> rows = Collections.singletonList(
                new String[]{"Updated Name", "1990-05-15", "NIC001"}
        );

        when(customerRepository.findByNicNumberIn(anyList())).thenReturn(Collections.singletonList(existingCustomer));
        when(customerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        BulkUploadResultDto result = new BulkUploadResultDto();
        bulkUploadService.processBatch(rows, 2, result);

        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(0);
        assertThat(existingCustomer.getName()).isEqualTo("Updated Name");
    }

    // processBatch – Validation error tests

    @Test
    @DisplayName("processBatch() - records error for rows with missing mandatory fields")
    void processBatch_missingFields_recordsError() {
        List<String[]> rows = Arrays.asList(
                new String[]{"", "1990-05-15", "NIC001"},   // missing name
                new String[]{"Alice", "", "NIC002"},          // missing DOB
                new String[]{"Bob",   "1990-05-15", ""}       // missing NIC
        );

        when(customerRepository.findByNicNumberIn(anyList())).thenReturn(Collections.emptyList());

        BulkUploadResultDto result = new BulkUploadResultDto();
        bulkUploadService.processBatch(rows, 2, result);

        assertThat(result.getFailedCount()).isEqualTo(3);
        assertThat(result.getErrors()).hasSize(3);
        assertThat(result.getSuccessCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("processBatch() - records error for invalid date format")
    void processBatch_invalidDate_recordsError() {
        List<String[]> rows = Collections.singletonList(
                new String[]{"Alice", "15-05-1990", "NIC001"} // wrong format
        );

        when(customerRepository.findByNicNumberIn(anyList())).thenReturn(Collections.emptyList());

        BulkUploadResultDto result = new BulkUploadResultDto();
        bulkUploadService.processBatch(rows, 2, result);

        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getErrors().get(0)).contains("Invalid date format");
    }

    @Test
    @DisplayName("processBatch() - handles insufficient columns gracefully")
    void processBatch_insufficientColumns_recordsError() {
        List<String[]> rows = Collections.singletonList(
                new String[]{"Alice", "1990-05-15"} // only 2 columns
        );

        when(customerRepository.findByNicNumberIn(anyList())).thenReturn(Collections.emptyList());

        BulkUploadResultDto result = new BulkUploadResultDto();
        bulkUploadService.processBatch(rows, 2, result);

        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getErrors().get(0)).contains("insufficient columns");
    }

    // Mixed batch (insert + update in same batch)

    @Test
    @DisplayName("processBatch() - correctly handles a mixed batch of inserts and updates")
    void processBatch_mixedBatch() {
        Customer existing = new Customer();
        existing.setId(5L);
        existing.setName("Old Name");
        existing.setDateOfBirth(LocalDate.of(1980, 1, 1));
        existing.setNicNumber("EXISTING_NIC");
        existing.setPhones(new ArrayList<>());
        existing.setAddresses(new ArrayList<>());
        existing.setFamilyMembers(new HashSet<>());

        List<String[]> rows = Arrays.asList(
                new String[]{"New Customer",      "1995-03-10", "NEW_NIC"},
                new String[]{"Updated Customer",  "1980-01-01", "EXISTING_NIC"}
        );

        when(customerRepository.findByNicNumberIn(anyList())).thenReturn(Collections.singletonList(existing));
        when(customerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        BulkUploadResultDto result = new BulkUploadResultDto();
        bulkUploadService.processBatch(rows, 2, result);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(0);
    }
}
