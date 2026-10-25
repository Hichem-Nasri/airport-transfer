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

    List<Booking> findByGroupIdAndIdNot(java.util.UUID groupId, Long id);

    boolean existsByAssignedDriverIdAndStatusIn(Long driverId, Collection<BookingStatus> statuses);

    // ---- statistics (pickup time inside [from, to))

    @Query("select b.status, count(b) from Booking b where b.pickupAt >= :from and b.pickupAt < :to group by b.status")
    List<Object[]> countByStatus(@Param("from") Instant from, @Param("to") Instant to);

    /** Per currency: number of bookings, charged amount, base amount in TND. Cancelled bookings are excluded. */
    @Query("select b.currency, count(b), sum(b.amount), sum(b.priceTnd) from Booking b "
        + "where b.pickupAt >= :from and b.pickupAt < :to and b.status <> :excluded "
        + "group by b.currency order by b.currency")
    List<Object[]> revenueByCurrency(@Param("from") Instant from, @Param("to") Instant to,
                                     @Param("excluded") BookingStatus excluded);

    @Query("select b.airport.id, b.airport.iataCode, count(b) from Booking b "
        + "where b.pickupAt >= :from and b.pickupAt < :to and b.status <> :excluded "
        + "group by b.airport.id, b.airport.iataCode order by count(b) desc, b.airport.iataCode")
    List<Object[]> countByAirport(@Param("from") Instant from, @Param("to") Instant to,
                                  @Param("excluded") BookingStatus excluded);
}
