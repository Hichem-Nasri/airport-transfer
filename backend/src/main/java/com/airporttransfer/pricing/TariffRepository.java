package com.airporttransfer.pricing;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    Optional<Tariff> findByAirportIdAndZoneIdAndVehicleTypeIdAndActiveTrue(Long airportId, Long zoneId,
                                                                           Long vehicleTypeId);

    long countByDemoTrue();
}
