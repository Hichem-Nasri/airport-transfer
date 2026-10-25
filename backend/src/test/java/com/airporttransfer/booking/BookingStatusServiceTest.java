package com.airporttransfer.booking;

import static com.airporttransfer.booking.BookingStatus.ASSIGNED;
import static com.airporttransfer.booking.BookingStatus.CANCELLED;
import static com.airporttransfer.booking.BookingStatus.COMPLETED;
import static com.airporttransfer.booking.BookingStatus.CONFIRMED;
import static com.airporttransfer.booking.BookingStatus.IN_PROGRESS;
import static com.airporttransfer.booking.BookingStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.airporttransfer.common.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookingStatusServiceTest {

    private final BookingStatusService service = new BookingStatusService();

    private static final Map<BookingStatus, Set<BookingStatus>> EXPECTED = Map.of(
        PENDING, Set.of(CONFIRMED, CANCELLED),
        CONFIRMED, Set.of(ASSIGNED, CANCELLED),
        ASSIGNED, Set.of(IN_PROGRESS, CANCELLED),
        IN_PROGRESS, Set.of(COMPLETED),
        COMPLETED, Set.of(),
        CANCELLED, Set.of());

    private static Booking booking(BookingStatus status) {
        Booking b = new Booking("MH-TEST0001", null, 1L, null, TripDirection.AIRPORT_TO_ADDRESS, null, "addr", null,
            Instant.now(), 1, 0, null, null, new BigDecimal("50.000"), "TND", null, new BigDecimal("50.000"), false,
            PaymentMethod.CASH);
        b.changeStatus(status);
        return b;
    }

    @Test
    void everyPairOfStatusesFollowsTheLifecycle() {
        for (BookingStatus from : BookingStatus.values()) {
            for (BookingStatus to : BookingStatus.values()) {
                assertThat(service.canTransition(from, to))
                    .as("%s -> %s", from, to)
                    .isEqualTo(EXPECTED.get(from).contains(to));
            }
        }
    }

    @Test
    void cancellationIsPossibleOnlyBeforeTheTripStarts() {
        assertThat(service.canTransition(PENDING, CANCELLED)).isTrue();
        assertThat(service.canTransition(CONFIRMED, CANCELLED)).isTrue();
        assertThat(service.canTransition(ASSIGNED, CANCELLED)).isTrue();
        assertThat(service.canTransition(IN_PROGRESS, CANCELLED)).isFalse();
        assertThat(service.canTransition(COMPLETED, CANCELLED)).isFalse();
        assertThat(service.canTransition(CANCELLED, CANCELLED)).isFalse();
    }

    @Test
    void transitionChangesTheStatusWhenLegal() {
        Booking booking = booking(PENDING);

        service.transition(booking, CONFIRMED);

        assertThat(booking.getStatus()).isEqualTo(CONFIRMED);
    }

    @Test
    void illegalTransitionIsRejectedAndLeavesTheStatusUntouched() {
        Booking booking = booking(IN_PROGRESS);

        assertThatThrownBy(() -> service.transition(booking, CANCELLED))
            .isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.getCode()).isEqualTo("INVALID_STATUS_TRANSITION");
                assertThat(e.getStatus().value()).isEqualTo(409);
            });
        assertThat(booking.getStatus()).isEqualTo(IN_PROGRESS);
    }

    @Test
    void stepsCannotBeSkipped() {
        assertThatThrownBy(() -> service.transition(booking(PENDING), IN_PROGRESS)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.transition(booking(PENDING), COMPLETED)).isInstanceOf(ApiException.class);
    }
}
