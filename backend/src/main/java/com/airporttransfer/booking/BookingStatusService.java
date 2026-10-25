package com.airporttransfer.booking;

import static com.airporttransfer.booking.BookingStatus.ASSIGNED;
import static com.airporttransfer.booking.BookingStatus.CANCELLED;
import static com.airporttransfer.booking.BookingStatus.COMPLETED;
import static com.airporttransfer.booking.BookingStatus.CONFIRMED;
import static com.airporttransfer.booking.BookingStatus.IN_PROGRESS;
import static com.airporttransfer.booking.BookingStatus.PENDING;

import com.airporttransfer.common.ApiException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * The only place that knows which status changes are legal:
 * PENDING → CONFIRMED → ASSIGNED → IN_PROGRESS → COMPLETED, and CANCELLED from any state before IN_PROGRESS.
 */
@Service
public class BookingStatusService {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED = new EnumMap<>(BookingStatus.class);

    static {
        ALLOWED.put(PENDING, EnumSet.of(CONFIRMED, CANCELLED));
        ALLOWED.put(CONFIRMED, EnumSet.of(ASSIGNED, CANCELLED));
        ALLOWED.put(ASSIGNED, EnumSet.of(IN_PROGRESS, CANCELLED));
        ALLOWED.put(IN_PROGRESS, EnumSet.of(COMPLETED));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(BookingStatus.class));
    }

    public boolean canTransition(BookingStatus from, BookingStatus to) {
        return ALLOWED.get(from).contains(to);
    }

    /** Applies the transition or throws 409 INVALID_STATUS_TRANSITION. */
    public void transition(Booking booking, BookingStatus to) {
        BookingStatus from = booking.getStatus();
        if (!canTransition(from, to)) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION",
                "A booking cannot go from " + from + " to " + to);
        }
        booking.changeStatus(to);
    }
}
