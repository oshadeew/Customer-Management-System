package com.customer.service;

import com.customer.dto.CountryDto;
import com.customer.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;
    private final ModelMapper modelMapper;

    public List<CountryDto> findAll() {
        return countryRepository.findAll()
                .stream()
                .map(c -> modelMapper.map(c, CountryDto.class))
                .collect(Collectors.toList());
    }
}
