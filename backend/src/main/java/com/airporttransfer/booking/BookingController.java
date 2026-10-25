package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.BookingDto;
import com.airporttransfer.booking.BookingDtos.BookingsCreatedDto;
import com.airporttransfer.booking.BookingDtos.CreateBookingRequest;
import com.airporttransfer.booking.BookingDtos.UpdateBookingRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in customer's own bookings. */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingsCreatedDto create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateBookingRequest request) {
        return service.create(userId(jwt), request);
    }

    @GetMapping
    public List<BookingDto> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.listMine(userId(jwt));
    }

    @GetMapping("/{id}")
    public BookingDto one(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getMine(userId(jwt), id);
    }

    @PutMapping("/{id}")
    public BookingDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                             @Valid @RequestBody UpdateBookingRequest request) {
        return service.update(Long.valueOf(jwt.getSubject()), id, request);
    }

    @PostMapping("/{id}/cancel")
    public BookingDto cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancel(userId(jwt), id);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
