package com.airporttransfer.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** Booking management by moderators and admins (the test transaction is rolled back). */
@Transactional
class StaffBookingIT extends OperationsTestSupport {

    private static final Instant IN_3_DAYS = Instant.now().plus(3, ChronoUnit.DAYS);

    private String staffToken() throws Exception {
        return token(account("moderator", Role.MODERATOR));
    }

    private String customerToken() throws Exception {
        return token(account("customer", Role.CUSTOMER));
    }

    // ---- access

    @Test
    void onlyModeratorsAndAdminsReachTheStaffApi() throws Exception {
        call("GET", customerToken(), "/api/staff/bookings", null).andExpect(status().isForbidden());
        call("GET", token(account("driver", Role.DRIVER)), "/api/staff/bookings", null).andExpect(status().isForbidden());
        call("GET", staffToken(), "/api/staff/bookings", null).andExpect(status().isOk());
        call("GET", token(account("admin", Role.ADMIN)), "/api/staff/bookings", null).andExpect(status().isOk());
    }

    @Test
    void moderatorsCannotUseAdminOnlyRoutes() throws Exception {
        call("POST", staffToken(), "/api/admin/users",
            "{\"email\":\"x@example.tn\",\"password\":\"secret-123\",\"fullName\":\"X\",\"role\":\"ADMIN\"}")
            .andExpect(status().isForbidden());
    }

    // ---- listing

    @Test
    void listIsPaginatedAndShowsCustomerAndDriver() throws Exception {
        String customer = customerToken();
        for (int i = 0; i < 3; i++) {
            book(customer, IN_3_DAYS.plus(i, ChronoUnit.DAYS));
        }
        String staff = staffToken();

        JsonNode page = ok("GET", staff, "/api/staff/bookings?size=2&q=customer-", null);

        assertThat(page.get("content")).hasSize(2);
        assertThat(page.get("totalElements").asLong()).isGreaterThanOrEqualTo(3);
        assertThat(page.get("size").asInt()).isEqualTo(2);
        JsonNode first = page.get("content").get(0);
        assertThat(first.get("customer").get("email").asText()).startsWith("customer-");
        assertThat(first.get("driver").isNull()).isTrue();
        assertThat(first.get("history").isNull()).isTrue();
        assertThat(first.get("booking").get("reference").asText()).startsWith("MH-");
    }

    @Test
    void listCanBeFilteredAndSearched() throws Exception {
        String customer = customerToken();
        long pending = book(customer, IN_3_DAYS);
        long confirmed = book(customer, IN_3_DAYS.plus(1, ChronoUnit.DAYS));
        String staff = staffToken();
        ok("POST", staff, "/api/staff/bookings/" + confirmed + "/confirm", null);
        String ref = ok("GET", staff, "/api/staff/bookings/" + pending, null).get("booking").get("reference").asText();

        assertThat(ids(ok("GET", staff, "/api/staff/bookings?status=CONFIRMED&q=customer-", null)))
            .contains(confirmed).doesNotContain(pending);
        assertThat(ids(ok("GET", staff, "/api/staff/bookings?q=" + ref.toLowerCase(), null))).containsExactly(pending);
        assertThat(ids(ok("GET", staff, "/api/staff/bookings?unassigned=true&q=customer-", null)))
            .contains(pending, confirmed);
        assertThat(ids(ok("GET", staff, "/api/staff/bookings?airportId=" + tun + "&q=customer-", null)))
            .contains(pending);
        assertThat(ids(ok("GET", staff, "/api/staff/bookings?airportId=999999", null))).isEmpty();

        String from = IN_3_DAYS.plus(12, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS).toString();
        assertThat(ids(ok("GET", staff, "/api/staff/bookings?q=customer-&from=" + from, null)))
            .contains(confirmed).doesNotContain(pending);
        String to = IN_3_DAYS.plus(12, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS).toString();
        assertThat(ids(ok("GET", staff, "/api/staff/bookings?q=customer-&to=" + to, null)))
            .contains(pending).doesNotContain(confirmed);
    }

    @Test
    void searchTextIsNotAPattern() throws Exception {
        book(customerToken(), IN_3_DAYS);

        assertThat(ids(ok("GET", staffToken(), "/api/staff/bookings?q=%25", null))).isEmpty();
        assertThat(ids(ok("GET", staffToken(), "/api/staff/bookings?q=_", null))).isEmpty();
    }

    private static List<Long> ids(JsonNode page) {
        List<Long> out = new ArrayList<>();
        page.get("content").forEach(n -> out.add(n.get("booking").get("id").asLong()));
        return out;
    }

    // ---- confirm, assign, cancel

    @Test
    void confirmThenAssignRecordsHistoryAndIsVisibleToTheCustomer() throws Exception {
        String customer = customerToken();
        long id = book(customer, IN_3_DAYS);
        User driver = account("driver", Role.DRIVER);
        String staff = staffToken();

        // A driver cannot be assigned before the booking is confirmed.
        call("POST", staff, "/api/staff/bookings/" + id + "/assign", "{\"driverId\":" + driver.getId() + "}")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BOOKING_NOT_ASSIGNABLE"));

        JsonNode confirmed = ok("POST", staff, "/api/staff/bookings/" + id + "/confirm", null);
        assertThat(confirmed.get("booking").get("status").asText()).isEqualTo("CONFIRMED");
        call("POST", staff, "/api/staff/bookings/" + id + "/confirm", null)
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        JsonNode assigned = ok("POST", staff, "/api/staff/bookings/" + id + "/assign",
            "{\"driverId\":" + driver.getId() + "}");
        assertThat(assigned.get("booking").get("status").asText()).isEqualTo("ASSIGNED");
        assertThat(assigned.get("driver").get("id").asLong()).isEqualTo(driver.getId());

        JsonNode detail = ok("GET", staff, "/api/staff/bookings/" + id, null);
        List<String> steps = new ArrayList<>();
        detail.get("history").forEach(h -> steps.add(h.get("toStatus").asText()));
        assertThat(steps).containsExactly("PENDING", "CONFIRMED", "ASSIGNED");
        assertThat(detail.get("history").get(1).get("actorRole").asText()).isEqualTo("MODERATOR");
        assertThat(detail.get("history").get(0).get("actorRole").asText()).isEqualTo("CUSTOMER");

        assertThat(ok("GET", customer, "/api/bookings/" + id, null).get("status").asText()).isEqualTo("ASSIGNED");
    }

    @Test
    void onlyActiveDriversCanBeAssigned() throws Exception {
        long id = book(customerToken(), IN_3_DAYS);
        String staff = staffToken();
        ok("POST", staff, "/api/staff/bookings/" + id + "/confirm", null);
        User notADriver = account("customer", Role.CUSTOMER);

        call("POST", staff, "/api/staff/bookings/" + id + "/assign", "{\"driverId\":" + notADriver.getId() + "}")
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("DRIVER_NOT_FOUND"));
        call("POST", staff, "/api/staff/bookings/" + id + "/assign", "{\"driverId\":999999}")
            .andExpect(status().isNotFound());
        call("POST", staff, "/api/staff/bookings/" + id + "/assign", "{}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void aDriverCannotTakeTwoTripsWithinTwoHoursUnlessForced() throws Exception {
        String customer = customerToken();
        String staff = staffToken();
        User driver = account("driver", Role.DRIVER);
        long first = book(customer, IN_3_DAYS);
        long nearby = book(customer, IN_3_DAYS.plus(90, ChronoUnit.MINUTES));
        long farAway = book(customer, IN_3_DAYS.plus(3, ChronoUnit.HOURS));
        for (long id : new long[] {first, nearby, farAway}) {
            ok("POST", staff, "/api/staff/bookings/" + id + "/confirm", null);
        }
        String body = "{\"driverId\":" + driver.getId() + "}";
        ok("POST", staff, "/api/staff/bookings/" + first + "/assign", body);

        call("POST", staff, "/api/staff/bookings/" + nearby + "/assign", body)
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DRIVER_UNAVAILABLE"));
        ok("POST", staff, "/api/staff/bookings/" + farAway + "/assign", body);
        JsonNode forced = ok("POST", staff, "/api/staff/bookings/" + nearby + "/assign",
            "{\"driverId\":" + driver.getId() + ",\"force\":true}");
        assertThat(forced.get("booking").get("status").asText()).isEqualTo("ASSIGNED");
    }

    @Test
    void reassigningChangesTheDriverAndKeepsTheStatus() throws Exception {
        long id = book(customerToken(), IN_3_DAYS);
        String staff = staffToken();
        User first = account("driver", Role.DRIVER);
        User second = account("driver", Role.DRIVER);
        ok("POST", staff, "/api/staff/bookings/" + id + "/confirm", null);
        ok("POST", staff, "/api/staff/bookings/" + id + "/assign", "{\"driverId\":" + first.getId() + "}");

        JsonNode again = ok("POST", staff, "/api/staff/bookings/" + id + "/assign",
            "{\"driverId\":" + second.getId() + "}");

        assertThat(again.get("booking").get("status").asText()).isEqualTo("ASSIGNED");
        assertThat(again.get("driver").get("id").asLong()).isEqualTo(second.getId());
        assertThat(again.get("history")).hasSize(4);
        assertThat(again.get("history").get(3).get("note").asText()).startsWith("Driver changed to");
    }

    @Test
    void driversListContainsOnlyActiveDrivers() throws Exception {
        User driver = account("driver", Role.DRIVER);
        account("customer", Role.CUSTOMER);
        JsonNode drivers = ok("GET", staffToken(), "/api/staff/drivers", null);

        List<Long> ids = new ArrayList<>();
        drivers.forEach(d -> ids.add(d.get("id").asLong()));
        assertThat(ids).contains(driver.getId());
        drivers.forEach(d -> assertThat(d.get("email").asText()).isNotEmpty());
    }

    @Test
    void staffCancelWithReasonIsRecordedAndVisibleToTheCustomer() throws Exception {
        String customer = customerToken();
        long id = book(customer, IN_3_DAYS);
        String staff = staffToken();

        JsonNode cancelled = ok("POST", staff, "/api/staff/bookings/" + id + "/cancel",
            "{\"reason\":\"No driver available\"}");

        assertThat(cancelled.get("booking").get("status").asText()).isEqualTo("CANCELLED");
        JsonNode last = cancelled.get("history").get(cancelled.get("history").size() - 1);
        assertThat(last.get("note").asText()).isEqualTo("No driver available");
        assertThat(last.get("actorRole").asText()).isEqualTo("MODERATOR");
        assertThat(ok("GET", customer, "/api/bookings/" + id, null).get("status").asText()).isEqualTo("CANCELLED");
        call("POST", staff, "/api/staff/bookings/" + id + "/cancel", null)
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void cancelWithoutBodyIsAllowedAndStaffCannotCancelAStartedTrip() throws Exception {
        long id = book(customerToken(), IN_3_DAYS);
        String staff = staffToken();
        User driver = account("driver", Role.DRIVER);
        ok("POST", staff, "/api/staff/bookings/" + id + "/confirm", null);
        ok("POST", staff, "/api/staff/bookings/" + id + "/assign", "{\"driverId\":" + driver.getId() + "}");
        ok("POST", staff, "/api/staff/bookings/" + id + "/start", null);

        call("POST", staff, "/api/staff/bookings/" + id + "/cancel", null).andExpect(status().isConflict());
        JsonNode done = ok("POST", staff, "/api/staff/bookings/" + id + "/complete", null);
        assertThat(done.get("booking").get("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    void unknownBookingIs404() throws Exception {
        call("GET", staffToken(), "/api/staff/bookings/999999", null)
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
    }

    @Test
    void customerCancellationIsInTheHistoryToo() throws Exception {
        String customer = customerToken();
        long id = book(customer, IN_3_DAYS);
        ok("POST", customer, "/api/bookings/" + id + "/cancel", null);

        JsonNode detail = ok("GET", staffToken(), "/api/staff/bookings/" + id, null);

        JsonNode last = detail.get("history").get(detail.get("history").size() - 1);
        assertThat(last.get("toStatus").asText()).isEqualTo("CANCELLED");
        assertThat(last.get("actorRole").asText()).isEqualTo("CUSTOMER");
    }
}
