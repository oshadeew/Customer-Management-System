package com.customer.service;

import com.customer.dto.*;
import com.customer.entity.*;
import com.customer.exception.DuplicateNicException;
import com.customer.exception.ResourceNotFoundException;
import com.customer.repository.CityRepository;
import com.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CityRepository cityRepository;

    // CREATE

    @Transactional
    public CustomerResponseDto create(CustomerRequestDto dto) {
        // Duplicate NIC check
        if (customerRepository.existsByNicNumber(dto.getNicNumber())) {
            throw new DuplicateNicException(dto.getNicNumber());
        }

        Customer customer = new Customer();
        mapRequestToEntity(dto, customer);
        Customer saved = customerRepository.save(customer);
        return toDetailResponse(saved);
    }

    // UPDATE

    @Transactional
    public CustomerResponseDto update(Long id, CustomerRequestDto dto) {
        Customer customer = customerRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));

        // If NIC is being changed, check uniqueness
        if (!customer.getNicNumber().equals(dto.getNicNumber())
                && customerRepository.existsByNicNumber(dto.getNicNumber())) {
            throw new DuplicateNicException(dto.getNicNumber());
        }

        // Clear existing children
        customer.getPhones().clear();
        customer.getAddresses().clear();
        customer.getFamilyMembers().clear();

        mapRequestToEntity(dto, customer);
        Customer saved = customerRepository.save(customer);
        return toDetailResponse(saved);
    }

    // GET SINGLE

    @Transactional(readOnly = true)
    public CustomerResponseDto findById(Long id) {
        Customer customer = customerRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        return toDetailResponse(customer);
    }

    // GET PAGINATED LIST

    @Transactional(readOnly = true)
    public Page<CustomerSummaryDto> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable)
                .map(this::toSummary);
    }

    // SEARCH BY NAME (for family member selector)

    @Transactional(readOnly = true)
    public List<CustomerSummaryDto> searchByName(String name) {
        return customerRepository.searchByName(name)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    // PRIVATE HELPERS

    /*
      Maps all fields from request DTO onto a Customer entity.
     */
    private void mapRequestToEntity(CustomerRequestDto dto, Customer customer) {
        customer.setName(dto.getName());
        customer.setDateOfBirth(dto.getDateOfBirth());
        customer.setNicNumber(dto.getNicNumber());

        // Phones
        if (dto.getPhones() != null) {
            for (PhoneRequestDto phoneDto : dto.getPhones()) {
                CustomerPhone phone = new CustomerPhone();
                phone.setCustomer(customer);
                phone.setPhoneNumber(phoneDto.getPhoneNumber());
                customer.getPhones().add(phone);
            }
        }

        // Addresses
        if (dto.getAddresses() != null) {
            for (AddressRequestDto addrDto : dto.getAddresses()) {
                City city = cityRepository.findById(addrDto.getCityId())
                        .orElseThrow(() -> new ResourceNotFoundException("City", addrDto.getCityId()));
                Address address = new Address();
                address.setCustomer(customer);
                address.setAddressLine1(addrDto.getAddressLine1());
                address.setAddressLine2(addrDto.getAddressLine2());
                address.setCity(city);
                customer.getAddresses().add(address);
            }
        }

        // Family Members
        if (dto.getFamilyMemberIds() != null && !dto.getFamilyMemberIds().isEmpty()) {
            List<Customer> members = customerRepository.findAllById(dto.getFamilyMemberIds());
            customer.getFamilyMembers().addAll(new HashSet<>(members));
        }
    }

    private CustomerResponseDto toDetailResponse(Customer customer) {
        CustomerResponseDto dto = new CustomerResponseDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setDateOfBirth(customer.getDateOfBirth());
        dto.setNicNumber(customer.getNicNumber());
        dto.setCreatedAt(customer.getCreatedAt());

        // Phones
        customer.getPhones().forEach(p -> {
            PhoneResponseDto pd = new PhoneResponseDto();
            pd.setId(p.getId());
            pd.setPhoneNumber(p.getPhoneNumber());
            dto.getPhones().add(pd);
        });

        // Addresses
        customer.getAddresses().forEach(a -> {
            AddressResponseDto ad = new AddressResponseDto();
            ad.setId(a.getId());
            ad.setAddressLine1(a.getAddressLine1());
            ad.setAddressLine2(a.getAddressLine2());
            if (a.getCity() != null) {
                ad.setCityId(a.getCity().getId());
                ad.setCityName(a.getCity().getName());
                if (a.getCity().getCountry() != null) {
                    ad.setCountryId(a.getCity().getCountry().getId());
                    ad.setCountryName(a.getCity().getCountry().getName());
                }
            }
            dto.getAddresses().add(ad);
        });

        // Family Members
        customer.getFamilyMembers().forEach(fm -> {
            FamilyMemberDto fmDto = new FamilyMemberDto();
            fmDto.setId(fm.getId());
            fmDto.setName(fm.getName());
            fmDto.setNicNumber(fm.getNicNumber());
            dto.getFamilyMembers().add(fmDto);
        });

        return dto;
    }

    private CustomerSummaryDto toSummary(Customer customer) {
        CustomerSummaryDto dto = new CustomerSummaryDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setNicNumber(customer.getNicNumber());
        dto.setDateOfBirth(customer.getDateOfBirth());
        dto.setCreatedAt(customer.getCreatedAt());
        return dto;
    }
}
