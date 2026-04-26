package com.customer.repository;

import com.customer.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    /*
      Load cities for a country, eagerly fetching country to avoid N+1.
     */
    @Query("SELECT c FROM City c JOIN FETCH c.country WHERE c.country.id = :countryId")
    List<City> findByCountryId(Long countryId);
}
