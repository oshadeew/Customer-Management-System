package com.customer.service;

import com.customer.dto.CityDto;
import com.customer.entity.City;
import com.customer.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    public List<CityDto> findByCountryId(Long countryId) {
        return cityRepository.findByCountryId(countryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private CityDto toDto(City city) {
        CityDto dto = new CityDto();
        dto.setId(city.getId());
        dto.setName(city.getName());
        dto.setCountryId(city.getCountry().getId());
        dto.setCountryName(city.getCountry().getName());
        return dto;
    }
}
