package com.airporttransfer.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {

    List<VehicleType> findAllByOrderByDisplayOrderAscIdAsc();

    List<VehicleType> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    Optional<VehicleType> findByIdAndActiveTrue(Long id);
}
