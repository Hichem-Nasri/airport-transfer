package com.airporttransfer.booking;

import com.airporttransfer.booking.StaffDtos.AssignDriverRequest;
import com.airporttransfer.booking.StaffDtos.BookingFilter;
import com.airporttransfer.booking.StaffDtos.CancelRequest;
import com.airporttransfer.booking.StaffDtos.StaffBookingDto;
import com.airporttransfer.common.PageDto;
import com.airporttransfer.user.UserDtos.PersonDto;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Booking management for MODERATOR and ADMIN (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/staff")
public class StaffBookingController {

    private final StaffBookingService service;

    public StaffBookingController(StaffBookingService service) {
        this.service = service;
    }

    @GetMapping("/bookings")
    public PageDto<StaffBookingDto> list(
        @RequestParam(required = false) BookingStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false) Long airportId,
        @RequestParam(required = false) Long driverId,
        @RequestParam(required = false) Boolean unassigned,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "asc") String order) {
        return service.list(new BookingFilter(status, from, to, airportId, driverId, unassigned, q), page, size,
            "desc".equalsIgnoreCase(order));
    }

    @GetMapping("/bookings/{id}")
    public StaffBookingDto one(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/drivers")
    public List<PersonDto> drivers() {
        return service.drivers();
    }

    @PostMapping("/bookings/{id}/confirm")
    public StaffBookingDto confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.confirm(id, Actor.from(jwt));
    }

    @PostMapping("/bookings/{id}/assign")
    public StaffBookingDto assign(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody AssignDriverRequest request) {
        return service.assign(id, request.driverId(), request.force(), Actor.from(jwt));
    }

    @PostMapping("/bookings/{id}/start")
    public StaffBookingDto start(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.start(id, Actor.from(jwt));
    }

    @PostMapping("/bookings/{id}/complete")
    public StaffBookingDto complete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.complete(id, Actor.from(jwt));
    }

    @PostMapping("/bookings/{id}/cancel")
    public StaffBookingDto cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody(required = false) CancelRequest request) {
        return service.cancel(id, request == null ? null : request.reason(), Actor.from(jwt));
    }
}
