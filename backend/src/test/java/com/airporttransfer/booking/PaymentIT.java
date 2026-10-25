package com.airporttransfer.booking;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.payment.PaymentRepository;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentIT extends OperationsTestSupport {

    @Autowired PaymentRepository payments;

    /** A booking confirmed and assigned to the driver; returns its id. */
    private long assigned(String customer, String moderator, User driver) throws Exception {
        long id = book(customer, Instant.now().plus(3, ChronoUnit.DAYS));
        ok("POST", moderator, "/api/staff/bookings/" + id + "/confirm", null);
        ok("POST", moderator, "/api/staff/bookings/" + id + "/assign", "{\"driverId\":%d}".formatted(driver.getId()));
        return id;
    }

    @Test
    void driverCollectsCashOnceTheTripHasStartedAndOnlyOnce() throws Exception {
        String customer = token(account("cus", Role.CUSTOMER));
        String moderator = token(account("mod", Role.MODERATOR));
        User driver = account("drv", Role.DRIVER);
        String driverToken = token(driver);
        long id = assigned(customer, moderator, driver);

        // Not before the trip starts.
        call("POST", driverToken, "/api/driver/trips/" + id + "/cash-collected", null)
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAYMENT_NOT_COLLECTABLE"));

        ok("POST", driverToken, "/api/driver/trips/" + id + "/start", null);
        var trip = ok("POST", driverToken, "/api/driver/trips/" + id + "/cash-collected", null);
        org.assertj.core.api.Assertions.assertThat(trip.get("booking").get("paymentStatus").asText()).isEqualTo("PAID");

        call("POST", driverToken, "/api/driver/trips/" + id + "/cash-collected", null)
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_PAID"));

        ok("POST", driverToken, "/api/driver/trips/" + id + "/complete", null);
        // The customer sees PAID on their booking and the ledger holds exactly one successful payment.
        var mine = ok("GET", customer, "/api/bookings/" + id, null);
        org.assertj.core.api.Assertions.assertThat(mine.get("paymentStatus").asText()).isEqualTo("PAID");
        org.assertj.core.api.Assertions.assertThat(payments.findByBookingIdOrderByIdAsc(id)).hasSize(1);
        call("GET", moderator, "/api/staff/bookings/" + id, null).andExpect(status().isOk())
            .andExpect(jsonPath("$.history[*].note", hasItem(containsString("Cash collected"))));
    }

    @Test
    void staffCanRecordCashAfterCompletionButNotOnAnotherDriversTrip() throws Exception {
        String customer = token(account("cus", Role.CUSTOMER));
        String moderator = token(account("mod", Role.MODERATOR));
        User driver = account("drv", Role.DRIVER);
        String driverToken = token(driver);
        String otherDriver = token(account("drv2", Role.DRIVER));
        long id = assigned(customer, moderator, driver);
        ok("POST", driverToken, "/api/driver/trips/" + id + "/start", null);
        ok("POST", driverToken, "/api/driver/trips/" + id + "/complete", null);

        call("POST", otherDriver, "/api/driver/trips/" + id + "/cash-collected", null)
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
        call("POST", customer, "/api/staff/bookings/" + id + "/cash-collected", null).andExpect(status().isForbidden());

        var staff = ok("POST", moderator, "/api/staff/bookings/" + id + "/cash-collected", null);
        org.assertj.core.api.Assertions.assertThat(staff.get("booking").get("paymentStatus").asText()).isEqualTo("PAID");
    }
}
