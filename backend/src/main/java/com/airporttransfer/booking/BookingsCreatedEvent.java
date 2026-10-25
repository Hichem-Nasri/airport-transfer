package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.BookingDto;
import java.util.List;
import java.util.Locale;

/** Published inside the creating transaction; the mail is sent only after it commits. */
public record BookingsCreatedEvent(String email, String fullName, Locale locale, List<BookingDto> bookings) {
}
