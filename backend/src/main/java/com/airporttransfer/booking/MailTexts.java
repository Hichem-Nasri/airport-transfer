package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.BookingDto;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.context.MessageSource;

/** Text pieces shared by the booking e-mails. Pickup times are shown in the time zone of the booking's airport. */
final class MailTexts {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private MailTexts() {
    }

    static String when(Instant instant, String timeZone) {
        return WHEN.format(instant.atZone(ZoneId.of(timeZone)));
    }

    static String airportName(BookingDto b, Locale locale) {
        return switch (locale.getLanguage()) {
            case "en" -> b.airport().nameEn();
            case "ar" -> b.airport().nameAr();
            default -> b.airport().nameFr();
        };
    }

    /** The multi-line description of one booking (reference, direction, airport, address, pickup, price). */
    static String leg(MessageSource messages, BookingDto b, Locale locale) {
        return messages.getMessage("mail.booking.leg", new Object[] {
            b.reference(),
            messages.getMessage("mail.direction." + b.direction(), null, locale),
            airportName(b, locale),
            b.address(),
            when(b.pickupAt(), b.airport().timeZone()),
            b.amount().toPlainString() + " " + b.currency()
        }, locale);
    }
}
