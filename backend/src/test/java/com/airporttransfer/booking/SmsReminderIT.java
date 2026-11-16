package com.airporttransfer.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.airporttransfer.notification.SmsGateway;
import com.airporttransfer.notification.SmsReminderService;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/** 24 h and 1 h SMS reminders against the real database; the gateway is replaced by a recorder. */
@Transactional
@Import(SmsReminderIT.Recorder.class)
class SmsReminderIT extends OperationsTestSupport {

    record Sms(String phone, String text) {
    }

    @TestConfiguration
    static class Recorder {
        static final List<Sms> SENT = new ArrayList<>();

        @Bean
        @Primary
        SmsGateway recordingGateway() {
            return (phone, text) -> SENT.add(new Sms(phone, text));
        }
    }

    @Autowired SmsReminderService reminders;
    @Autowired BookingRepository bookings;
    @Autowired UserRepository userRepo;
    @Autowired MessageSource messageSource;

    @BeforeEach
    void clear() {
        Recorder.SENT.clear();
    }

    /** A CONFIRMED booking whose pickup is {@code lead} from now. */
    private Booking confirmedIn(Duration lead) throws Exception {
        User customer = account("sms", Role.CUSTOMER);
        long id = book(token(customer), Instant.now().plus(Duration.ofDays(3)));
        Booking b = bookings.findById(id).orElseThrow();
        b.applyCustomerEdit(b.getAddress(), null, Instant.now().plus(lead), 1, null);
        b.changeStatus(BookingStatus.CONFIRMED);
        bookings.saveAndFlush(b);
        return b;
    }

    private List<Sms> forBooking(Booking b) {
        return Recorder.SENT.stream().filter(s -> s.text().contains(b.getReference())).toList();
    }

    @Test
    void firstReminderIsSentOnceInTheLast24Hours() throws Exception {
        Booking b = confirmedIn(Duration.ofHours(23));
        reminders.sendDue(Instant.now());
        reminders.sendDue(Instant.now());
        assertThat(forBooking(b)).hasSize(1);
        assertThat(forBooking(b).get(0).phone()).isEqualTo("+216 20 111 222");
        assertThat(bookings.findById(b.getId()).orElseThrow().getReminder24hSentAt()).isNotNull();
    }

    @Test
    void nothingIsSentWhenThePickupIsMoreThan24HoursAway() throws Exception {
        Booking b = confirmedIn(Duration.ofHours(30));
        reminders.sendDue(Instant.now());
        assertThat(forBooking(b)).isEmpty();
    }

    @Test
    void lastMinuteBookingOnlyGetsTheOneHourReminder() throws Exception {
        Booking b = confirmedIn(Duration.ofMinutes(30));
        reminders.sendDue(Instant.now());
        assertThat(forBooking(b)).hasSize(1);
        assertThat(bookings.findById(b.getId()).orElseThrow().getReminder24hSentAt()).isNull();
        assertThat(bookings.findById(b.getId()).orElseThrow().getReminder1hSentAt()).isNotNull();
    }

    @Test
    void bothRemindersAreSentOverTime() throws Exception {
        Booking b = confirmedIn(Duration.ofHours(23));
        reminders.sendDue(Instant.now());
        reminders.sendDue(Instant.now().plus(Duration.ofHours(22).plusMinutes(30)));
        assertThat(forBooking(b)).hasSize(2);
    }

    @Test
    void pendingAndCancelledBookingsAreNotReminded() throws Exception {
        Booking pending = confirmedIn(Duration.ofHours(10));
        pending.changeStatus(BookingStatus.PENDING);
        Booking cancelled = confirmedIn(Duration.ofHours(10));
        cancelled.changeStatus(BookingStatus.CANCELLED);
        bookings.flush();
        reminders.sendDue(Instant.now());
        assertThat(forBooking(pending)).isEmpty();
        assertThat(forBooking(cancelled)).isEmpty();
    }

    @Test
    void aFailingGatewayLeavesTheReminderForTheNextRun() throws Exception {
        Booking b = confirmedIn(Duration.ofHours(5));
        SmsReminderService failing = new SmsReminderService(bookings, userRepo, (p, t) -> {
            throw new IllegalStateException("provider down");
        }, messageSource);
        assertThat(failing.sendDue(Instant.now())).isZero();
        assertThat(bookings.findById(b.getId()).orElseThrow().getReminder24hSentAt()).isNull();
    }
}
