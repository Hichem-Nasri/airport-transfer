package com.airporttransfer.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    boolean existsByReference(String reference);

    @EntityGraph(attributePaths = {"airport", "zone", "vehicleType"})
    List<Booking> findByCustomerIdOrderByPickupAtDescIdDesc(Long customerId);

    @EntityGraph(attributePaths = {"airport", "zone", "vehicleType"})
    Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);

    /** Staff list: filtered, paginated, with the display relations loaded in the same query. */
    @Override
    @EntityGraph(attributePaths = {"airport", "zone", "vehicleType"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"airport", "zone", "vehicleType"})
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"airport", "zone", "vehicleType"})
    List<Booking> findByAssignedDriverIdAndStatusIn(Long driverId, Collection<BookingStatus> statuses, Sort sort);

    @EntityGraph(attributePaths = {"airport", "zone", "vehicleType"})
    Optional<Booking> findByIdAndAssignedDriverId(Long id, Long driverId);

    /** Does the driver already have another active trip whose pickup is inside the window? */
    boolean existsByAssignedDriverIdAndIdNotAndStatusInAndPickupAtBetween(
        Long driverId, Long excludedBookingId, Collection<BookingStatus> statuses, Instant from, Instant to);
}
