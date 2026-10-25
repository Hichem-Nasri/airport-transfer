package com.airporttransfer.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AirportRepository extends JpaRepository<Airport, Long> {

    List<Airport> findAllByOrderByIataCodeAsc();

    List<Airport> findAllByActiveTrueOrderByIataCodeAsc();

    Optional<Airport> findByIdAndActiveTrue(Long id);
}
