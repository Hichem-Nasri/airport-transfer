package com.airporttransfer.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class BookingEditIT extends OperationsTestSupport {

    private ResultActions edit(String token, long id, Instant pickupAt, String address) throws Exception {
        ObjectNode body = json.createObjectNode();
        body.put("address", address);
        body.put("flightNumber", "tu 714");
        body.put("pickupAt", pickupAt.truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("luggageCount", 3);
        body.put("notes", "Ring the bell");
        return mvc.perform(put("/api/bookings/" + id).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(body.toString()));
    }

    @Test
    void customerEditsDetailsWhilePendingAndPriceStaysTheSame() throws Exception {
        String token = token(account("cus", Role.CUSTOMER));
        long id = book(token, Instant.now().plus(3, ChronoUnit.DAYS));
        JsonNode before = json.readTree(mvc.perform(get("/api/bookings/" + id).header("Authorization", "Bearer " + token))
            .andReturn().getResponse().getContentAsString());

        JsonNode after = json.readTree(edit(token, id, Instant.now().plus(4, ChronoUnit.DAYS),
                "  1 Avenue Habib Bourguiba  ")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.address").value("1 Avenue Habib Bourguiba"))
            .andExpect(jsonPath("$.flightNumber").value("TU714"))
            .andExpect(jsonPath("$.luggageCount").value(3))
            .andExpect(jsonPath("$.editable").value(true))
            .andReturn().getResponse().getContentAsString());
        assertThat(after.get("amount").decimalValue()).isEqualByComparingTo(before.get("amount").decimalValue());
        assertThat(after.get("priceTnd").decimalValue()).isEqualByComparingTo(before.get("priceTnd").decimalValue());
        assertThat(after.get("pickupAt").asText()).isNotEqualTo(before.get("pickupAt").asText());
    }

    @Test
    void editIsRefusedOnceADriverIsAssignedOrForPastTimesOrOtherPeoplesBookings() throws Exception {
        String token = token(account("cus", Role.CUSTOMER));
        String other = token(account("oth", Role.CUSTOMER));
        String moderator = token(account("mod", Role.MODERATOR));
        User driver = account("drv", Role.DRIVER);
        long id = book(token, Instant.now().plus(3, ChronoUnit.DAYS));

        edit(token, id, Instant.now().minus(1, ChronoUnit.HOURS), "x").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PICKUP_IN_PAST"));
        edit(other, id, Instant.now().plus(4, ChronoUnit.DAYS), "x").andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));

        ok("POST", moderator, "/api/staff/bookings/" + id + "/confirm", null);
        edit(token, id, Instant.now().plus(4, ChronoUnit.DAYS), "Confirmed edit").andExpect(status().isOk());

        ok("POST", moderator, "/api/staff/bookings/" + id + "/assign", "{\"driverId\":%d}".formatted(driver.getId()));
        edit(token, id, Instant.now().plus(4, ChronoUnit.DAYS), "Too late").andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_EDITABLE"));

        mvc.perform(get("/api/staff/bookings/" + id).header("Authorization", "Bearer " + moderator))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.history[*].note", org.hamcrest.Matchers.hasItem(containsString("edited"))));
    }

    @Test
    void roundTripLegsKeepTheirOrder() throws Exception {
        String token = token(account("cus", Role.CUSTOMER));
        Instant out = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant back = Instant.now().plus(6, ChronoUnit.DAYS);
        ObjectNode request = bookingRequest(out);
        request.put("returnPickupAt", back.truncatedTo(ChronoUnit.SECONDS).toString());
        JsonNode created = json.readTree(mvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request.toString()))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long outbound = created.get("bookings").get(0).get("id").asLong();
        long inbound = created.get("bookings").get(1).get("id").asLong();

        // The outbound leg cannot move after the return leg, nor the return leg before the outbound one.
        edit(token, outbound, back.plus(1, ChronoUnit.DAYS), "x").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("RETURN_BEFORE_OUTBOUND"));
        edit(token, inbound, out.minus(1, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES), "x")
            .andExpect(status().isBadRequest());
        edit(token, outbound, out.plus(1, ChronoUnit.DAYS), "Moved outbound").andExpect(status().isOk());
        edit(token, inbound, back.plus(1, ChronoUnit.DAYS), "Moved return").andExpect(status().isOk());
    }
}
