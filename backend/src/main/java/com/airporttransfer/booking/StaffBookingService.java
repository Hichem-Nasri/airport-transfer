package com.airporttransfer.booking;

import com.airporttransfer.booking.StaffDtos.BookingFilter;
import com.airporttransfer.booking.StaffDtos.StaffBookingDto;
import com.airporttransfer.common.ApiException;
import com.airporttransfer.payment.Payment;
import com.airporttransfer.payment.PaymentService;
import com.airporttransfer.common.PageDto;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserDtos.PersonDto;
import com.airporttransfer.user.UserMapper;
import com.airporttransfer.user.UserRepository;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What moderators and admins can do with any booking. Role access is enforced at route level (/api/staff/**). */
@Service
public class StaffBookingService {

    /** A driver cannot take two trips whose pickups are closer than this, unless staff force it. */
    static final Duration DRIVER_BUFFER = Duration.ofHours(2);
    private static final Set<BookingStatus> DRIVER_BUSY = Set.of(BookingStatus.ASSIGNED, BookingStatus.IN_PROGRESS);
    static final int MAX_PAGE_SIZE = 100;

    private final BookingRepository bookings;
    private final UserRepository users;
    private final UserMapper userMapper;
    private final BookingWorkflowService workflow;
    private final BookingViews views;
    private final PaymentService payments;

    public StaffBookingService(BookingRepository bookings, UserRepository users, UserMapper userMapper,
                               BookingWorkflowService workflow, BookingViews views, PaymentService payments) {
        this.payments = payments;
        this.bookings = bookings;
        this.users = users;
        this.userMapper = userMapper;
        this.workflow = workflow;
        this.views = views;
    }

    @Transactional(readOnly = true)
    public PageDto<StaffBookingDto> list(BookingFilter filter, int page, int size, boolean latestFirst) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort sort = Sort.by(latestFirst ? Sort.Direction.DESC : Sort.Direction.ASC, "pickupAt").and(Sort.by("id"));
        var result = bookings.findAll(BookingSpecifications.matching(filter),
            PageRequest.of(Math.max(page, 0), safeSize, sort));
        return PageDto.of(result, content -> views.staff(content, false));
    }

    @Transactional(readOnly = true)
    public StaffBookingDto get(Long id) {
        return views.staff(List.of(find(id)), true).get(0);
    }

    @Transactional(readOnly = true)
    public List<PersonDto> drivers() {
        return users.findByRoleAndEnabledTrueOrderByFullNameAsc(Role.DRIVER).stream().map(userMapper::toPerson).toList();
    }

    @Transactional
    public StaffBookingDto confirm(Long id, Actor actor) {
        return act(id, b -> workflow.changeStatus(b, BookingStatus.CONFIRMED, actor, null));
    }

    @Transactional
    public StaffBookingDto assign(Long id, Long driverId, boolean force, Actor actor) {
        Booking booking = find(id);
        User driver = users.findById(driverId)
            .filter(u -> u.getRole() == Role.DRIVER && u.isEnabled())
            .orElseThrow(() -> ApiException.notFound("DRIVER_NOT_FOUND", "Unknown or inactive driver"));
        boolean busy = bookings.existsByAssignedDriverIdAndIdNotAndStatusInAndPickupAtBetween(driver.getId(),
            booking.getId(), DRIVER_BUSY, booking.getPickupAt().minus(DRIVER_BUFFER),
            booking.getPickupAt().plus(DRIVER_BUFFER));
        if (busy && !force) {
            throw ApiException.conflict("DRIVER_UNAVAILABLE",
                "This driver has another trip within " + DRIVER_BUFFER.toHours() + " hours of this pickup");
        }
        workflow.assignDriver(booking, driver, actor);
        return views.staff(List.of(booking), true).get(0);
    }

    @Transactional
    public StaffBookingDto start(Long id, Actor actor) {
        return act(id, b -> workflow.changeStatus(b, BookingStatus.IN_PROGRESS, actor, null));
    }

    @Transactional
    public StaffBookingDto complete(Long id, Actor actor) {
        return act(id, b -> workflow.changeStatus(b, BookingStatus.COMPLETED, actor, null));
    }

    /** Staff records the cash the driver handed over (for example when the driver forgot to do it). */
    @Transactional
    public StaffBookingDto cashCollected(Long id, Actor actor) {
        return act(id, b -> {
            Payment payment = payments.collectCash(b, actor.userId());
            workflow.recordEdited(b, actor, "Cash collected: " + payment.getAmount().toPlainString() + " " + payment.getCurrency());
        });
    }

    @Transactional
    public StaffBookingDto cancel(Long id, String reason, Actor actor) {
        String note = reason == null || reason.isBlank() ? null : reason.trim();
        return act(id, b -> workflow.changeStatus(b, BookingStatus.CANCELLED, actor, note));
    }

    private StaffBookingDto act(Long id, java.util.function.Consumer<Booking> action) {
        Booking booking = find(id);
        action.accept(booking);
        return views.staff(List.of(booking), true).get(0);
    }

    private Booking find(Long id) {
        return bookings.findDetailedById(id)
            .orElseThrow(() -> ApiException.notFound("BOOKING_NOT_FOUND", "Booking not found"));
    }
}
