package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.BookingDto;
import com.airporttransfer.booking.BookingEvents.DriverAssigned;
import com.airporttransfer.booking.BookingEvents.StatusChanged;
import com.airporttransfer.common.ApiException;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserMapper;
import com.airporttransfer.user.UserRepository;
import java.util.Locale;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Every status change of a booking goes through here, whoever asks (customer, staff, driver): the rule check
 * ({@link BookingStatusService}), the history row, and the notification event. Callers own the transaction.
 */
@Service
public class BookingWorkflowService {

    private static final Set<BookingStatus> NOTIFIED = Set.of(
        BookingStatus.CONFIRMED, BookingStatus.CANCELLED, BookingStatus.COMPLETED);

    private final BookingStatusService statuses;
    private final BookingStatusHistoryRepository history;
    private final BookingRepository bookings;
    private final BookingMapper mapper;
    private final UserRepository users;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher events;

    public BookingWorkflowService(BookingStatusService statuses, BookingStatusHistoryRepository history,
                                  BookingRepository bookings, BookingMapper mapper, UserRepository users,
                                  UserMapper userMapper, ApplicationEventPublisher events) {
        this.statuses = statuses;
        this.history = history;
        this.bookings = bookings;
        this.mapper = mapper;
        this.users = users;
        this.userMapper = userMapper;
        this.events = events;
    }

    public void recordCreated(Booking booking, Actor actor) {
        history.save(new BookingStatusHistory(booking.getId(), null, BookingStatus.PENDING, actor, null));
    }

    /** Applies the transition (409 if illegal), records it, and notifies when the customer or driver should know. */
    public void changeStatus(Booking booking, BookingStatus to, Actor actor, String note) {
        BookingStatus from = booking.getStatus();
        statuses.transition(booking, to);
        history.save(new BookingStatusHistory(booking.getId(), from, to, actor, note));
        bookings.saveAndFlush(booking);
        if (NOTIFIED.contains(to)) {
            BookingDto dto = mapper.toDto(booking);
            events.publishEvent(new StatusChanged(dto, person(booking.getCustomerId()), locale(booking),
                driverOf(booking), note, actor.role()));
        }
    }

    /** Sets the driver. From CONFIRMED the booking becomes ASSIGNED; from ASSIGNED only the driver changes. */
    public void assignDriver(Booking booking, User driver, Actor actor) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.assignDriver(driver.getId());
            changeStatus(booking, BookingStatus.ASSIGNED, actor, "Driver: " + driver.getFullName());
        } else if (booking.getStatus() == BookingStatus.ASSIGNED) {
            booking.assignDriver(driver.getId());
            history.save(new BookingStatusHistory(booking.getId(), BookingStatus.ASSIGNED, BookingStatus.ASSIGNED,
                actor, "Driver changed to " + driver.getFullName()));
            bookings.saveAndFlush(booking);
        } else {
            throw ApiException.conflict("BOOKING_NOT_ASSIGNABLE",
                "A driver can only be assigned to a CONFIRMED or ASSIGNED booking");
        }
        events.publishEvent(new DriverAssigned(mapper.toDto(booking), person(booking.getCustomerId()),
            locale(booking), userMapper.toPerson(driver), booking.getLuggageCount()));
    }

    private com.airporttransfer.user.UserDtos.PersonDto person(Long userId) {
        return users.findById(userId).map(userMapper::toPerson).orElse(null);
    }

    private com.airporttransfer.user.UserDtos.PersonDto driverOf(Booking booking) {
        return booking.getAssignedDriverId() == null ? null : person(booking.getAssignedDriverId());
    }

    private static Locale locale(Booking booking) {
        return Locale.forLanguageTag(booking.getCustomerLanguage());
    }
}
