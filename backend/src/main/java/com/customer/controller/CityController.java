package com.customer.controller;

import com.customer.dto.CityDto;
import com.customer.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CityController {

    private final CityService cityService;

    /*
      GET /api/cities/{countryId} - Return cities filtered by country.
     */
    @GetMapping("/{countryId}")
    public ResponseEntity<List<CityDto>> findByCountry(@PathVariable Long countryId) {
        return ResponseEntity.ok(cityService.findByCountryId(countryId));
    }
}
