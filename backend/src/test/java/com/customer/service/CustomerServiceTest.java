package com.customer.service;

import com.customer.dto.CustomerRequestDto;
import com.customer.dto.CustomerResponseDto;
import com.customer.entity.Customer;
import com.customer.exception.DuplicateNicException;
import com.customer.exception.ResourceNotFoundException;
import com.customer.repository.CityRepository;
import com.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer sampleCustomer;
    private CustomerRequestDto requestDto;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setId(1L);
        sampleCustomer.setName("Alice Fernando");
        sampleCustomer.setDateOfBirth(LocalDate.of(1990, 5, 15));
        sampleCustomer.setNicNumber("901234567V");
        sampleCustomer.setCreatedAt(LocalDateTime.now());
        sampleCustomer.setPhones(new ArrayList<>());
        sampleCustomer.setAddresses(new ArrayList<>());
        sampleCustomer.setFamilyMembers(new HashSet<>());

        requestDto = new CustomerRequestDto();
        requestDto.setName("Alice Fernando");
        requestDto.setDateOfBirth(LocalDate.of(1990, 5, 15));
        requestDto.setNicNumber("901234567V");
    }

    // CREATE tests

    @Test
    @DisplayName("create() - success: saves new customer and returns response DTO")
    void create_success() {
        when(customerRepository.existsByNicNumber("901234567V")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerResponseDto response = customerService.create(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Alice Fernando");
        assertThat(response.getNicNumber()).isEqualTo("901234567V");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("create() - throws DuplicateNicException when NIC already exists")
    void create_duplicateNic_throwsException() {
        when(customerRepository.existsByNicNumber("901234567V")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(requestDto))
                .isInstanceOf(DuplicateNicException.class)
                .hasMessageContaining("901234567V");

        verify(customerRepository, never()).save(any());
    }

    // UPDATE tests

    @Test
    @DisplayName("update() - success: updates customer fields")
    void update_success() {
        CustomerRequestDto updateDto = new CustomerRequestDto();
        updateDto.setName("Alice Updated");
        updateDto.setDateOfBirth(LocalDate.of(1990, 6, 20));
        updateDto.setNicNumber("901234567V"); // same NIC

        when(customerRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerResponseDto response = customerService.update(1L, updateDto);

        assertThat(response).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("update() - throws ResourceNotFoundException when customer not found")
    void update_notFound_throwsException() {
        when(customerRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(99L, requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update() - throws DuplicateNicException when new NIC belongs to another customer")
    void update_changeNicToDuplicate_throwsException() {
        sampleCustomer.setNicNumber("OLD_NIC_123");
        requestDto.setNicNumber("EXISTING_NIC_XYZ"); // different NIC that already exists

        when(customerRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.existsByNicNumber("EXISTING_NIC_XYZ")).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(1L, requestDto))
                .isInstanceOf(DuplicateNicException.class)
                .hasMessageContaining("EXISTING_NIC_XYZ");
    }

    // FIND BY ID tests

    @Test
    @DisplayName("findById() - success: returns full customer response")
    void findById_success() {
        when(customerRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleCustomer));

        CustomerResponseDto response = customerService.findById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice Fernando");
    }

    @Test
    @DisplayName("findById() - throws ResourceNotFoundException when customer not found")
    void findById_notFound_throwsException() {
        when(customerRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // PAGINATED LIST tests

    @Test
    @DisplayName("findAll() - returns paginated summary list")
    void findAll_returnsPaginatedList() {
        List<Customer> customers = Arrays.asList(sampleCustomer);
        Page<Customer> page = new PageImpl<>(customers, PageRequest.of(0, 10), 1);
        when(customerRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<com.customer.dto.CustomerSummaryDto> result = customerService.findAll(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Alice Fernando");
    }
}
