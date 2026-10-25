package com.airporttransfer.admin;

import com.airporttransfer.admin.StatsDtos.StatsDto;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ROLE_ADMIN is enforced in SecurityConfig. Default period: 30 days back to 30 days ahead. */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final StatsService stats;

    public AdminStatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping
    public StatsDto stats(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return stats.stats(from, to, Instant.now());
    }
}
