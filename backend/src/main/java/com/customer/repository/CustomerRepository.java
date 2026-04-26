package com.customer.repository;

import com.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /*
      Find by ID with all associations loaded in minimal queries.
     */
    @EntityGraph(attributePaths = {
        "phones",
        "addresses",
        "addresses.city",
        "addresses.city.country",
        "familyMembers"
    })
    @Query("SELECT c FROM Customer c WHERE c.id = :id")
    Optional<Customer> findByIdWithDetails(@Param("id") Long id);

    boolean existsByNicNumber(String nicNumber);

    Optional<Customer> findByNicNumber(String nicNumber);

    /*
      Paginated list
     */
    Page<Customer> findAll(Pageable pageable);

    /*
      Search by name for the family member selector.
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> searchByName(@Param("name") String name);

    /*
      Find all by NIC numbers in one query
     */
    @Query("SELECT c FROM Customer c WHERE c.nicNumber IN :nics")
    List<Customer> findByNicNumberIn(@Param("nics") List<String> nics);
}
