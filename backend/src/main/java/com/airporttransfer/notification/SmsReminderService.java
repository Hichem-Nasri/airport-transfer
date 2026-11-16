package com.airporttransfer.notification;

import com.airporttransfer.booking.Booking;
import com.airporttransfer.booking.BookingRepository;
import com.airporttransfer.booking.BookingStatus;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends the 24 h and 1 h pickup reminders by SMS. Only CONFIRMED or ASSIGNED bookings are reminded. The 24 h reminder
 * goes out when the pickup is between 2 h and 24 h away (a booking made later than that only gets the 1 h one); the
 * 1 h reminder when the pickup is less than 1 h away. Each reminder is marked on the booking and sent at most once.
 * A failed send leaves the marker empty, so the next run retries it. Meant for a single backend instance.
 */
@Service
public class SmsReminderService {

    static final Set<BookingStatus> REMINDED = Set.of(BookingStatus.CONFIRMED, BookingStatus.ASSIGNED);
    static final Duration FIRST_WINDOW = Duration.ofHours(24);
    static final Duration FIRST_MIN_LEAD = Duration.ofHours(2);
    static final Duration SECOND_WINDOW = Duration.ofHours(1);

    private static final Logger LOG = LoggerFactory.getLogger(SmsReminderService.class);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BookingRepository bookings;
    private final UserRepository users;
    private final SmsGateway gateway;
    private final MessageSource messages;

    public SmsReminderService(BookingRepository bookings, UserRepository users, SmsGateway gateway,
                              MessageSource messages) {
        this.bookings = bookings;
        this.users = users;
        this.gateway = gateway;
        this.messages = messages;
    }

    /** Sends every reminder that is due at {@code now}; returns how many were sent. */
    @Transactional
    public int sendDue(Instant now) {
        int sent = 0;
        List<Booking> first = bookings.findDueFirstReminder(REMINDED, now.plus(FIRST_MIN_LEAD), now.plus(FIRST_WINDOW));
        for (Booking b : first) {
            if (send(b, "sms.reminder.24h")) {
                b.markFirstReminderSent(now);
                sent++;
            }
        }
        List<Booking> second = bookings.findDueSecondReminder(REMINDED, now, now.plus(SECOND_WINDOW));
        for (Booking b : second) {
            if (send(b, "sms.reminder.1h")) {
                b.markSecondReminderSent(now);
                sent++;
            }
        }
        return sent;
    }

    private boolean send(Booking b, String key) {
        User customer = users.findById(b.getCustomerId()).orElse(null);
        if (customer == null || !customer.isEnabled() || customer.getPhone() == null || customer.getPhone().isBlank()) {
            return false;
        }
        Locale locale = Locale.forLanguageTag(b.getCustomerLanguage());
        String airport = switch (locale.getLanguage()) {
            case "en" -> b.getAirport().getNameEn();
            case "ar" -> b.getAirport().getNameAr();
            default -> b.getAirport().getNameFr();
        };
        String when = WHEN.format(b.getPickupAt().atZone(ZoneId.of(b.getAirport().getTimeZone())));
        String text = messages.getMessage(key, new Object[] {b.getReference(), when, airport}, locale);
        try {
            gateway.send(customer.getPhone().trim(), text);
            return true;
        } catch (RuntimeException e) {
            LOG.warn("SMS reminder for booking {} failed, will retry: {}", b.getReference(), e.getMessage());
            return false;
        }
    }
}
