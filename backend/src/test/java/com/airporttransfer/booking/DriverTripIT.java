package com.airporttransfer.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DriverTripIT extends OperationsTestSupport {

    private static final Instant IN_3_DAYS = Instant.now().plus(3, ChronoUnit.DAYS);

    /** Books, confirms and assigns a trip; returns its id. */
    private long assignedTrip(String customer, User driver, Instant pickup) throws Exception {
        long id = book(customer, pickup);
        String staff = token(account("moderator", Role.MODERATOR));
        ok("POST", staff, "/api/staff/bookings/" + id + "/confirm", null);
        ok("POST", staff, "/api/staff/bookings/" + id + "/assign", "{\"driverId\":" + driver.getId() + "}");
        return id;
    }

    @Test
    void driverSeesOnlyTheirOwnAssignedTripsSoonestFirst() throws Exception {
        String customer = token(account("customer", Role.CUSTOMER));
        User driver = account("driver", Role.DRIVER);
        User other = account("driver", Role.DRIVER);
        long later = assignedTrip(customer, driver, IN_3_DAYS.plus(1, ChronoUnit.DAYS));
        long sooner = assignedTrip(customer, driver, IN_3_DAYS);
        assignedTrip(customer, other, IN_3_DAYS.plus(5, ChronoUnit.HOURS));
        book(customer, IN_3_DAYS); // not assigned to anybody

        JsonNode trips = ok("GET", token(driver), "/api/driver/trips", null);

        assertThat(trips).hasSize(2);
        assertThat(trips.get(0).get("booking").get("id").asLong()).isEqualTo(sooner);
        assertThat(trips.get(1).get("booking").get("id").asLong()).isEqualTo(later);
        assertThat(trips.get(0).get("customer").get("fullName").asText()).isEqualTo("customer Name");
        assertThat(trips.get(0).get("customer").get("phone").asText()).isNotEmpty();
        assertThat(trips.get(0).get("booking").get("amount").decimalValue()).isEqualByComparingTo("50.000");
        assertThat(ok("GET", token(other), "/api/driver/trips", null)).hasSize(1);
    }

    @Test
    void driverRunsATripFromStartToCompletion() throws Exception {
        String customer = token(account("customer", Role.CUSTOMER));
        User driver = account("driver", Role.DRIVER);
        String token = token(driver);
        long id = assignedTrip(customer, driver, IN_3_DAYS);

        // Cannot complete a trip that has not started.
        call("POST", token, "/api/driver/trips/" + id + "/complete", null)
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        JsonNode started = ok("POST", token, "/api/driver/trips/" + id + "/start", null);
        assertThat(started.get("booking").get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(ok("GET", token, "/api/driver/trips", null)).hasSize(1);
        assertThat(ok("GET", customer, "/api/bookings/" + id, null).get("status").asText()).isEqualTo("IN_PROGRESS");
        call("POST", token, "/api/driver/trips/" + id + "/start", null).andExpect(status().isConflict());

        JsonNode done = ok("POST", token, "/api/driver/trips/" + id + "/complete", null);
        assertThat(done.get("booking").get("status").asText()).isEqualTo("COMPLETED");
        assertThat(ok("GET", token, "/api/driver/trips", null)).isEmpty();
        assertThat(ok("GET", token, "/api/driver/trips?scope=past", null)).hasSize(1);
    }

    @Test
    void anotherDriversTripLooksMissing() throws Exception {
        String customer = token(account("customer", Role.CUSTOMER));
        User driver = account("driver", Role.DRIVER);
        String intruder = token(account("driver", Role.DRIVER));
        long id = assignedTrip(customer, driver, IN_3_DAYS);

        call("GET", intruder, "/api/driver/trips/" + id, null)
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
        call("POST", intruder, "/api/driver/trips/" + id + "/start", null).andExpect(status().isNotFound());
        call("POST", intruder, "/api/driver/trips/" + id + "/complete", null).andExpect(status().isNotFound());
    }

    @Test
    void cancelledTripsMoveToThePastList() throws Exception {
        String customer = token(account("customer", Role.CUSTOMER));
        User driver = account("driver", Role.DRIVER);
        long id = assignedTrip(customer, driver, IN_3_DAYS);
        ok("POST", customer, "/api/bookings/" + id + "/cancel", null);

        assertThat(ok("GET", token(driver), "/api/driver/trips", null)).isEmpty();
        JsonNode past = ok("GET", token(driver), "/api/driver/trips?scope=past", null);
        assertThat(past).hasSize(1);
        assertThat(past.get(0).get("booking").get("status").asText()).isEqualTo("CANCELLED");
    }

    @Test
    void onlyDriversUseTheDriverApi() throws Exception {
        call("GET", token(account("customer", Role.CUSTOMER)), "/api/driver/trips", null).andExpect(status().isForbidden());
        call("GET", token(account("moderator", Role.MODERATOR)), "/api/driver/trips", null).andExpect(status().isForbidden());
    }
}
