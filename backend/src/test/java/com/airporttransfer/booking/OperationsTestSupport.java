package com.airporttransfer.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.AbstractIT;
import com.airporttransfer.catalog.AirportRepository;
import com.airporttransfer.catalog.VehicleTypeRepository;
import com.airporttransfer.catalog.ZoneRepository;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Shared helpers of the staff and driver integration tests: accounts, tokens, bookings. */
abstract class OperationsTestSupport extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserService users;
    @Autowired AirportRepository airports;
    @Autowired ZoneRepository zones;
    @Autowired VehicleTypeRepository vehicleTypes;

    Long tun;
    Long z01;
    Long sedan;

    @BeforeEach
    void catalogIds() {
        tun = airports.findAll().stream().filter(a -> a.getIataCode().equals("TUN")).findFirst().orElseThrow().getId();
        z01 = zones.findAll().stream().filter(z -> z.getCode().equals("Z01")).findFirst().orElseThrow().getId();
        sedan = vehicleTypes.findAll().stream().filter(v -> v.getCode().equals("SEDAN")).findFirst().orElseThrow().getId();
    }

    static String email(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.tn";
    }

    /** Creates an account with the role and returns its id; the password is "secret-123". */
    User account(String prefix, Role role) {
        return users.create(email(prefix), "secret-123", prefix + " Name", "+216 20 111 222", role);
    }

    String token(User user) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"secret-123\"}".formatted(user.getEmail())))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    ObjectNode bookingRequest(Instant pickupAt) {
        ObjectNode n = json.createObjectNode();
        n.put("airportId", tun);
        n.put("direction", "AIRPORT_TO_ADDRESS");
        n.put("zoneId", z01);
        n.put("address", "12 Rue de Marseille, Tunis");
        n.put("pickupAt", pickupAt.truncatedTo(ChronoUnit.SECONDS).toString());
        n.put("passengers", 2);
        n.put("luggageCount", 1);
        n.put("vehicleTypeId", sedan);
        n.put("currency", "TND");
        n.put("paymentMethod", "CASH");
        return n;
    }

    /** A customer books; returns the booking id. */
    long book(String customerToken, Instant pickupAt) throws Exception {
        String res = mvc.perform(post("/api/bookings").header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON).content(bookingRequest(pickupAt).toString()))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("bookings").get(0).get("id").asLong();
    }

    ResultActions call(String method, String token, String url, String body) throws Exception {
        var builder = method.equals("GET") ? get(url) : post(url);
        builder.header("Authorization", "Bearer " + token);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(builder);
    }

    JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    JsonNode ok(String method, String token, String url, String body) throws Exception {
        return read(call(method, token, url, body).andExpect(status().isOk()));
    }
}
