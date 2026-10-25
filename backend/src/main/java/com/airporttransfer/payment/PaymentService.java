package com.airporttransfer.payment;

import com.airporttransfer.booking.Booking;
import com.airporttransfer.booking.BookingStatus;
import com.airporttransfer.booking.PaymentMethod;
import com.airporttransfer.booking.PaymentStatus;
import com.airporttransfer.common.ApiException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment rules. Callers own the transaction and the booking's history note. Cash can be collected once the trip has
 * started (the driver is with the customer) and never twice.
 */
@Service
public class PaymentService {

    private static final Set<BookingStatus> COLLECTABLE = Set.of(BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);

    private final PaymentRepository payments;
    private final Map<PaymentMethod, PaymentGateway> gateways = new EnumMap<>(PaymentMethod.class);

    public PaymentService(PaymentRepository payments, List<PaymentGateway> gateways) {
        this.payments = payments;
        gateways.forEach(g -> this.gateways.put(g.method(), g));
    }

    /** Payment methods that have a gateway; used to refuse a method nobody can collect. */
    public boolean isAvailable(PaymentMethod method) {
        return gateways.containsKey(method);
    }

    /** Records the cash handed to the driver: the exact snapshot amount, once. */
    @Transactional
    public Payment collectCash(Booking booking, Long recordedBy) {
        if (booking.getPaymentMethod() != PaymentMethod.CASH) {
            throw ApiException.conflict("PAYMENT_METHOD_MISMATCH", "This booking is not paid in cash");
        }
        if (!COLLECTABLE.contains(booking.getStatus())) {
            throw ApiException.conflict("PAYMENT_NOT_COLLECTABLE",
                "Cash can only be collected once the trip has started");
        }
        if (booking.getPaymentStatus() != PaymentStatus.UNPAID) {
            throw ApiException.conflict("ALREADY_PAID", "This booking is already paid");
        }
        PaymentGateway gateway = gateways.get(PaymentMethod.CASH);
        Payment payment = payments.saveAndFlush(new Payment(booking.getId(), gateway.provider(), booking.getAmount(),
            booking.getCurrency(), null, recordedBy));
        booking.markPaid(Instant.now());
        return payment;
    }

    /** Called when a booking is cancelled: a paid booking is refunded through its gateway, an unpaid one is left alone. */
    @Transactional
    public void refundIfPaid(Booking booking) {
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            return;
        }
        payments.findByBookingIdAndStatus(booking.getId(), Payment.Status.SUCCEEDED).ifPresent(payment -> {
            PaymentGateway gateway = gateways.get(booking.getPaymentMethod());
            payment.markRefunded(gateway.refund(payment));
            payments.save(payment);
        });
        booking.markRefunded();
    }
}
