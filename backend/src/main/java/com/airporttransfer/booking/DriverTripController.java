package com.airporttransfer.booking;

import com.airporttransfer.booking.StaffDtos.DriverTripDto;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in driver's own trips (ROLE_DRIVER, enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/driver/trips")
public class DriverTripController {

    private final DriverTripService service;

    public DriverTripController(DriverTripService service) {
        this.service = service;
    }

    @GetMapping
    public List<DriverTripDto> mine(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(defaultValue = "upcoming") String scope) {
        return service.list(Actor.from(jwt).userId(), "past".equalsIgnoreCase(scope));
    }

    @GetMapping("/{id}")
    public DriverTripDto one(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(Actor.from(jwt).userId(), id);
    }

    @PostMapping("/{id}/start")
    public DriverTripDto start(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.start(Actor.from(jwt).userId(), id);
    }

    @PostMapping("/{id}/cash-collected")
    public DriverTripDto cashCollected(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cashCollected(Actor.from(jwt).userId(), id);
    }

    @PostMapping("/{id}/complete")
    public DriverTripDto complete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.complete(Actor.from(jwt).userId(), id);
    }
}
