package com.airporttransfer.booking;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {

    List<BookingStatusHistory> findByBookingIdOrderByCreatedAtAscIdAsc(Long bookingId);
}
