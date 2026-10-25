package com.airporttransfer.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingIdAndStatus(Long bookingId, Payment.Status status);

    List<Payment> findByBookingIdOrderByIdAsc(Long bookingId);
}
