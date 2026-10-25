package com.airporttransfer.admin;

import com.airporttransfer.booking.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class StatsDtos {

    private StatsDtos() {
    }

    /** Charged amount of the non-cancelled bookings in one currency, plus their base price in TND. */
    public record CurrencyRevenue(String currency, long bookings, BigDecimal amount, BigDecimal amountTnd) {
    }

    public record AirportCount(Long airportId, String iataCode, long bookings) {
    }

    /** Statistics over the bookings whose pickup time is in [from, to). */
    public record StatsDto(Instant from, Instant to, long totalBookings, long activeBookings,
                           Map<BookingStatus, Long> byStatus, List<CurrencyRevenue> revenue,
                           List<AirportCount> byAirport) {
    }
}
