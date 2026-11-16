package com.airporttransfer.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.AbstractIT;
import com.airporttransfer.catalog.AirportRepository;
import com.airporttransfer.catalog.VehicleTypeRepository;
import com.airporttransfer.catalog.ZoneRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Linked France + Tunisia packs against the seeded data: Paris Eco = EUR 150, TUN to Z01 Sedan = demo TND tariff. */
@Transactional
class PackIT extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AirportRepository airports;
    @Autowired ZoneRepository zones;
    @Autowired VehicleTypeRepository vehicleTypes;
    @Autowired BookingRepository bookings;

    Long ory;
    Long tun;
    Long paris;
    Long z01;
    Long eco;
    Long sedan;

    @BeforeEach
    void ids() {
        ory = airports.findAll().stream().filter(a -> a.getIataCode().equals("ORY")).findFirst().orElseThrow().getId();
        tun = airports.findAll().stream().filter(a -> a.getIataCode().equals("TUN")).findFirst().orElseThrow().getId();
        paris = zones.findAll().stream().filter(z -> z.getCode().equals("FR01")).findFirst().orElseThrow().getId();
        z01 = zones.findAll().stream().filter(z -> z.getCode().equals("Z01")).findFirst().orElseThrow().getId();
        eco = vehicleTypes.findAll().stream().filter(v -> v.getCode().equals("ECO")).findFirst().orElseThrow().getId();
        sedan = vehicleTypes.findAll().stream().filter(v -> v.getCode().equals("SEDAN")).findFirst().orElseThrow().getId();
    }

    private String token() throws Exception {
        String body = """
            {"email":"p-%s@example.fr","password":"secret-123","fullName":"Pack Customer"}
            """.formatted(UUID.randomUUID());
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    private ObjectNode leg(Long airport, String direction, Long zone, Long vehicle, int daysAhead) {
        ObjectNode n = json.createObjectNode();
        n.put("airportId", airport);
        n.put("direction", direction);
        n.put("zoneId", zone);
        n.put("address", "1 Rue Test");
        n.put("pickupAt", Instant.now().plus(daysAhead, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString());
        n.put("passengers", 2);
        n.put("luggageCount", 1);
        n.put("vehicleTypeId", vehicle);
        return n;
    }

    private ObjectNode pack(ObjectNode... legs) {
        ObjectNode n = json.createObjectNode();
        ArrayNode arr = n.putArray("legs");
        for (ObjectNode l : legs) {
            arr.add(l);
        }
        n.put("currency", "EUR");
        n.put("paymentMethod", "CASH");
        return n;
    }

    private ResultActions send(String token, ObjectNode body) throws Exception {
        return mvc.perform(post("/api/bookings/pack").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(body.toString()));
    }

    @Test
    void packCreatesLinkedBookingsInOrderWithOneGroup() throws Exception {
        String res = send(token(), pack(
            leg(ory, "ADDRESS_TO_AIRPORT", paris, eco, 5),
            leg(tun, "AIRPORT_TO_ADDRESS", z01, sedan, 5).put("pickupAt",
                Instant.now().plus(5, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS).toString())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.groupId").isNotEmpty())
            .andExpect(jsonPath("$.bookings.length()").value(2))
            .andReturn().getResponse().getContentAsString();
        JsonNode created = json.readTree(res).get("bookings");
        assertThat(created.get(0).get("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(created.get(0).get("currency").asText()).isEqualTo("EUR");
        assertThat(created.get(1).get("currency").asText()).isEqualTo("EUR");
        assertThat(created.get(0).get("id").asLong()).isLessThan(created.get(1).get("id").asLong());
        assertThat(bookings.findAll().stream().filter(b -> b.getGroupId() != null).count()).isEqualTo(2);
    }

    @Test
    void legsMustFollowEachOtherInTime() throws Exception {
        send(token(), pack(
            leg(ory, "ADDRESS_TO_AIRPORT", paris, eco, 6),
            leg(tun, "AIRPORT_TO_ADDRESS", z01, sedan, 5)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("LEGS_OUT_OF_ORDER"));
    }

    @Test
    void firstLegMustBeInTheFuture() throws Exception {
        send(token(), pack(
            leg(ory, "ADDRESS_TO_AIRPORT", paris, eco, -1),
            leg(tun, "AIRPORT_TO_ADDRESS", z01, sedan, 5)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PICKUP_IN_PAST"));
    }

    @Test
    void aPackNeedsAtLeastTwoLegs() throws Exception {
        send(token(), pack(leg(ory, "ADDRESS_TO_AIRPORT", paris, eco, 5)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void packQuoteTotalIsTheSumOfTheLegs() throws Exception {
        ObjectNode body = json.createObjectNode();
        body.put("currency", "EUR");
        ArrayNode legs = body.putArray("legs");
        legs.addObject().put("airportId", ory).put("zoneId", paris).put("vehicleTypeId", eco);
        legs.addObject().put("airportId", tun).put("zoneId", z01).put("vehicleTypeId", sedan);
        String res = mvc.perform(post("/api/public/pack-quote").contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode q = json.readTree(res);
        BigDecimal sum = q.get("legs").get(0).get("amount").decimalValue()
            .add(q.get("legs").get(1).get("amount").decimalValue());
        assertThat(q.get("totalAmount").decimalValue()).isEqualByComparingTo(sum);
        assertThat(q.get("legs").get(0).get("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("150.00"));
    }
}
