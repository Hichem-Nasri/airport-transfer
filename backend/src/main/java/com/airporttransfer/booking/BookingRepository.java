package com.airporttransfer.booking;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByReference(String reference);

    @EntityGraph(attributePaths = {"airport", "zone", "vehicleType"})
    List<Booking> findByCustomerIdOrderByPickupAtDescIdDesc(Long customerId);

    @EntityGraph(attributePaths = {"airport", "zone", "vehicleType"})
    Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);
}
