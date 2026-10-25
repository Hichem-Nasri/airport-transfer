package com.airporttransfer.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.airporttransfer.admin.StatsDtos.StatsDto;
import com.airporttransfer.booking.BookingRepository;
import com.airporttransfer.booking.BookingStatus;
import com.airporttransfer.common.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatsServiceTest {

    BookingRepository bookings = mock(BookingRepository.class);
    StatsService service = new StatsService(bookings);
    Instant now = Instant.parse("2026-10-25T10:00:00Z");

    @Test
    void aggregatesStatusesRevenuePerCurrencyAndAirports() {
        when(bookings.countByStatus(any(), any())).thenReturn(List.of(
            new Object[] {BookingStatus.COMPLETED, 3L}, new Object[] {BookingStatus.CANCELLED, 2L},
            new Object[] {BookingStatus.PENDING, 1L}));
        when(bookings.collectedByCurrency(any(), any(), any())).thenReturn(List.<Object[]>of(new Object[] {"EUR", new BigDecimal("45.00")}));
        when(bookings.revenueByCurrency(any(), any(), any())).thenReturn(List.of(
            new Object[] {"EUR", 2L, new BigDecimal("90.00"), new BigDecimal("300.000")},
            new Object[] {"TND", 2L, new BigDecimal("150.000"), new BigDecimal("150.000")}));
        when(bookings.countByAirport(any(), any(), any())).thenReturn(List.<Object[]>of(new Object[] {1L, "TUN", 4L}));

        StatsDto s = service.stats(null, null, now);

        assertThat(s.totalBookings()).isEqualTo(6);
        assertThat(s.activeBookings()).isEqualTo(4);
        assertThat(s.byStatus().get(BookingStatus.ASSIGNED)).isZero();
        assertThat(s.byStatus().get(BookingStatus.COMPLETED)).isEqualTo(3);
        assertThat(s.revenue()).hasSize(2);
        assertThat(s.revenue().get(0).currency()).isEqualTo("EUR");
        assertThat(s.revenue().get(0).amount()).isEqualByComparingTo("90.00");
        assertThat(s.revenue().get(0).collected()).isEqualByComparingTo("45.00");
        assertThat(s.revenue().get(1).collected()).isEqualByComparingTo("0");
        assertThat(s.byAirport().get(0).iataCode()).isEqualTo("TUN");
        assertThat(s.from()).isEqualTo(now.minus(StatsService.DEFAULT_PAST));
        assertThat(s.to()).isEqualTo(now.plus(StatsService.DEFAULT_FUTURE));
    }

    @Test
    void rejectsEmptyOrTooLongPeriods() {
        assertThatThrownBy(() -> service.stats(now, now, now)).isInstanceOf(ApiException.class)
            .extracting("code").isEqualTo("INVALID_RANGE");
        assertThatThrownBy(() -> service.stats(now.minusSeconds(400L * 86400), now, now))
            .isInstanceOf(ApiException.class).extracting("code").isEqualTo("INVALID_RANGE");
    }
}
