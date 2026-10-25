package com.airporttransfer.admin;

import com.airporttransfer.admin.StatsDtos.AirportCount;
import com.airporttransfer.admin.StatsDtos.CurrencyRevenue;
import com.airporttransfer.admin.StatsDtos.StatsDto;
import com.airporttransfer.booking.BookingRepository;
import com.airporttransfer.booking.BookingStatus;
import com.airporttransfer.common.ApiException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dashboard figures. Revenue excludes cancelled bookings; amounts stay in the currency charged (never mixed). */
@Service
public class StatsService {

    static final Duration DEFAULT_PAST = Duration.ofDays(30);
    static final Duration DEFAULT_FUTURE = Duration.ofDays(30);
    static final Duration MAX_RANGE = Duration.ofDays(366);

    private final BookingRepository bookings;

    public StatsService(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public StatsDto stats(Instant from, Instant to, Instant now) {
        Instant start = from != null ? from : now.minus(DEFAULT_PAST);
        Instant end = to != null ? to : now.plus(DEFAULT_FUTURE);
        if (!start.isBefore(end) || Duration.between(start, end).compareTo(MAX_RANGE) > 0) {
            throw ApiException.badRequest("INVALID_RANGE", "The period must be positive and at most 366 days");
        }
        Map<BookingStatus, Long> byStatus = new EnumMap<>(BookingStatus.class);
        for (BookingStatus s : BookingStatus.values()) {
            byStatus.put(s, 0L);
        }
        long total = 0;
        for (Object[] row : bookings.countByStatus(start, end)) {
            long count = (Long) row[1];
            byStatus.put((BookingStatus) row[0], count);
            total += count;
        }
        long active = total - byStatus.get(BookingStatus.CANCELLED);
        List<CurrencyRevenue> revenue = bookings.revenueByCurrency(start, end, BookingStatus.CANCELLED).stream()
            .map(r -> new CurrencyRevenue((String) r[0], (Long) r[1], (BigDecimal) r[2], (BigDecimal) r[3]))
            .toList();
        List<AirportCount> airports = bookings.countByAirport(start, end, BookingStatus.CANCELLED).stream()
            .map(r -> new AirportCount((Long) r[0], (String) r[1], (Long) r[2]))
            .toList();
        return new StatsDto(start, end, total, active, byStatus, revenue, airports);
    }
}
