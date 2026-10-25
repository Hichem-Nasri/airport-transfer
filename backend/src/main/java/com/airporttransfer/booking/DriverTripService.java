package com.airporttransfer.booking;

import com.airporttransfer.booking.StaffDtos.DriverTripDto;
import com.airporttransfer.common.ApiException;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A driver's own trips. Someone else's trip looks exactly like a missing one. */
@Service
public class DriverTripService {

    private static final Set<BookingStatus> UPCOMING = Set.of(BookingStatus.ASSIGNED, BookingStatus.IN_PROGRESS);
    private static final Set<BookingStatus> PAST = Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED);

    private final BookingRepository bookings;
    private final BookingWorkflowService workflow;
    private final BookingMapper mapper;
    private final BookingViews views;

    public DriverTripService(BookingRepository bookings, BookingWorkflowService workflow, BookingMapper mapper,
                             BookingViews views) {
        this.bookings = bookings;
        this.workflow = workflow;
        this.mapper = mapper;
        this.views = views;
    }

    /** {@code upcoming}: ASSIGNED and IN_PROGRESS, soonest first. {@code past}: COMPLETED and CANCELLED, latest first. */
    @Transactional(readOnly = true)
    public List<DriverTripDto> list(Long driverId, boolean past) {
        Sort sort = Sort.by(past ? Sort.Direction.DESC : Sort.Direction.ASC, "pickupAt").and(Sort.by("id"));
        return bookings.findByAssignedDriverIdAndStatusIn(driverId, past ? PAST : UPCOMING, sort).stream()
            .map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public DriverTripDto get(Long driverId, Long id) {
        return view(find(driverId, id));
    }

    @Transactional
    public DriverTripDto start(Long driverId, Long id) {
        Booking booking = find(driverId, id);
        workflow.changeStatus(booking, BookingStatus.IN_PROGRESS, new Actor(driverId, com.airporttransfer.user.Role.DRIVER), null);
        return view(booking);
    }

    @Transactional
    public DriverTripDto complete(Long driverId, Long id) {
        Booking booking = find(driverId, id);
        workflow.changeStatus(booking, BookingStatus.COMPLETED, new Actor(driverId, com.airporttransfer.user.Role.DRIVER), null);
        return view(booking);
    }

    private DriverTripDto view(Booking booking) {
        return new DriverTripDto(mapper.toDto(booking), views.customer(booking));
    }

    private Booking find(Long driverId, Long id) {
        return bookings.findByIdAndAssignedDriverId(id, driverId)
            .orElseThrow(() -> ApiException.notFound("TRIP_NOT_FOUND", "Trip not found"));
    }
}
