package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.BookingDto;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.UserDtos.PersonDto;
import java.util.Locale;

/** Events published inside the transaction; the mails go out only after it commits. */
public final class BookingEvents {

    private BookingEvents() {
    }

    /** CONFIRMED, CANCELLED or COMPLETED. {@code driver} is null when none is assigned. */
    public record StatusChanged(BookingDto booking, PersonDto customer, Locale customerLocale, PersonDto driver,
                                String note, Role actorRole) {
    }

    /** A driver was assigned (or replaced). */
    public record DriverAssigned(BookingDto booking, PersonDto customer, Locale customerLocale, PersonDto driver,
                                 int luggageCount) {
    }
}
