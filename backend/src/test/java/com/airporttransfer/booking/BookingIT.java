package com.airporttransfer.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.AbstractIT;
import com.airporttransfer.catalog.AirportRepository;
import com.airporttransfer.catalog.VehicleTypeRepository;
import com.airporttransfer.catalog.ZoneRepository;
import com.airporttransfer.pricing.ExchangeRate;
import com.airporttransfer.pricing.ExchangeRateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Bookings against the seeded demo data (the test transaction is rolled back).
 * Demo prices: TUN → Z01 SEDAN = 50.000 TND, demo EUR rate 3.400000 TND per EUR.
 */
@Transactional
class BookingIT extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AirportRepository airports;
    @Autowired ZoneRepository zones;
    @Autowired VehicleTypeRepository vehicleTypes;
    @Autowired ExchangeRateRepository rates;
    @Autowired BookingRepository bookings;

    Long tun;
    Long z01;
    Long sedan;
    Long van;

    @BeforeEach
    void ids() {
        tun = airports.findAll().stream().filter(a -> a.getIataCode().equals("TUN")).findFirst().orElseThrow().getId();
        z01 = zones.findAll().stream().filter(z -> z.getCode().equals("Z01")).findFirst().orElseThrow().getId();
        sedan = vehicleTypes.findAll().stream().filter(v -> v.getCode().equals("SEDAN")).findFirst().orElseThrow().getId();
        van = vehicleTypes.findAll().stream().filter(v -> v.getCode().equals("VAN")).findFirst().orElseThrow().getId();
    }

    // ---- helpers

    private String newCustomerToken() throws Exception {
        String body = """
            {"email":"c-%s@example.tn","password":"secret-123","fullName":"Customer"}
            """.formatted(UUID.randomUUID());
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    private ObjectNode request() {
        ObjectNode n = json.createObjectNode();
        n.put("airportId", tun);
        n.put("direction", "AIRPORT_TO_ADDRESS");
        n.put("zoneId", z01);
        n.put("address", "12 Rue de Marseille, Tunis");
        n.put("flightNumber", "tu 714");
        n.put("pickupAt", Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString());
        n.put("passengers", 2);
        n.put("luggageCount", 2);
        n.put("vehicleTypeId", sedan);
        n.put("currency", "TND");
        n.put("paymentMethod", "CASH");
        return n;
    }

    private ResultActions create(String token, ObjectNode body) throws Exception {
        return mvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(body.toString()));
    }

    private JsonNode createOk(String token, ObjectNode body) throws Exception {
        String res = create(token, body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    private JsonNode getJson(String token, String url) throws Exception {
        String res = mvc.perform(get(url).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    private static void assertDecimal(JsonNode node, String expected) {
        assertThat(node.decimalValue()).isEqualByComparingTo(new BigDecimal(expected));
    }

    // ---- creating

    @Test
    void oneWayBookingStoresThePriceSnapshotInTnd() throws Exception {
        String token = newCustomerToken();
        ObjectNode req = request();

        JsonNode created = createOk(token, req);

        assertThat(created.get("groupId").isNull()).isTrue();
        assertThat(created.get("bookings")).hasSize(1);
        JsonNode b = created.get("bookings").get(0);
        assertThat(b.get("reference").asText()).matches("MH-[A-Z2-9]{8}");
        assertThat(b.get("status").asText()).isEqualTo("PENDING");
        assertThat(b.get("direction").asText()).isEqualTo("AIRPORT_TO_ADDRESS");
        assertThat(b.get("flightNumber").asText()).isEqualTo("TU714");
        assertThat(b.get("airport").get("iataCode").asText()).isEqualTo("TUN");
        assertThat(b.get("zone").get("code").asText()).isEqualTo("Z01");
        assertThat(b.get("vehicleType").get("code").asText()).isEqualTo("SEDAN");
        assertThat(Instant.parse(b.get("pickupAt").asText())).isEqualTo(Instant.parse(req.get("pickupAt").asText()));
        assertDecimal(b.get("priceTnd"), "50.000");
        assertThat(b.get("currency").asText()).isEqualTo("TND");
        assertThat(b.get("exchangeRate").isNull()).isTrue();
        assertDecimal(b.get("amount"), "50.000");
        assertThat(b.get("priceDemo").asBoolean()).isTrue();
        assertThat(b.get("cancellable").asBoolean()).isTrue();

        Booking stored = bookings.findById(b.get("id").asLong()).orElseThrow();
        assertThat(stored.getAmount()).isEqualByComparingTo("50.000");
        assertThat(stored.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    void eurBookingSnapshotSurvivesLaterRateChanges() throws Exception {
        String token = newCustomerToken();
        ObjectNode req = request();
        req.put("currency", "eur");

        JsonNode b = createOk(token, req).get("bookings").get(0);

        // 50.000 TND / 3.400000 = 14.70588... → 14.71 (HALF_UP, 2 decimals)
        assertThat(b.get("currency").asText()).isEqualTo("EUR");
        assertDecimal(b.get("priceTnd"), "50.000");
        assertDecimal(b.get("exchangeRate"), "3.4");
        assertDecimal(b.get("amount"), "14.71");

        ExchangeRate newRate = new ExchangeRate();
        newRate.setCurrencyCode("EUR");
        newRate.setTndPerUnit(new BigDecimal("4.000000"));
        newRate.setDemo(false);
        rates.saveAndFlush(newRate);

        JsonNode again = getJson(token, "/api/bookings/" + b.get("id").asLong());
        assertDecimal(again.get("exchangeRate"), "3.4");
        assertDecimal(again.get("amount"), "14.71");

        JsonNode fresh = createOk(token, req).get("bookings").get(0);
        assertDecimal(fresh.get("exchangeRate"), "4");
        assertDecimal(fresh.get("amount"), "12.50");
    }

    @Test
    void roundTripCreatesTwoLinkedBookingsWithTheSamePrice() throws Exception {
        String token = newCustomerToken();
        ObjectNode req = request();
        req.put("returnPickupAt", Instant.now().plus(6, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString());
        req.put("returnFlightNumber", "tu 715");

        JsonNode created = createOk(token, req);

        JsonNode out = created.get("bookings").get(0);
        JsonNode back = created.get("bookings").get(1);
        assertThat(created.get("groupId").asText()).isNotEmpty();
        assertThat(out.get("groupId").asText()).isEqualTo(created.get("groupId").asText());
        assertThat(back.get("groupId").asText()).isEqualTo(created.get("groupId").asText());
        assertThat(out.get("direction").asText()).isEqualTo("AIRPORT_TO_ADDRESS");
        assertThat(back.get("direction").asText()).isEqualTo("ADDRESS_TO_AIRPORT");
        assertThat(out.get("reference").asText()).isNotEqualTo(back.get("reference").asText());
        assertThat(back.get("flightNumber").asText()).isEqualTo("TU715");
        assertThat(Instant.parse(back.get("pickupAt").asText())).isEqualTo(Instant.parse(req.get("returnPickupAt").asText()));
        assertDecimal(out.get("amount"), "50.000");
        assertDecimal(back.get("amount"), "50.000");
        assertThat(getJson(token, "/api/bookings")).hasSize(2);
    }

    @Test
    void previewAndChargedAmountsComeFromTheSameCalculation() throws Exception {
        String token = newCustomerToken();
        ObjectNode req = request();
        req.put("currency", "EUR");
        req.put("returnPickupAt", Instant.now().plus(6, ChronoUnit.DAYS).toString());

        String preview = mvc.perform(get("/api/public/booking-quote?airportId=" + tun + "&zoneId=" + z01
                + "&vehicleTypeId=" + sedan + "&currency=EUR&roundTrip=true"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode quote = json.readTree(preview);
        JsonNode created = createOk(token, req).get("bookings");

        assertThat(quote.get("legs").asInt()).isEqualTo(2);
        assertDecimal(quote.get("leg").get("amount"), "14.71");
        assertDecimal(quote.get("totalAmount"), "29.42");
        BigDecimal charged = created.get(0).get("amount").decimalValue().add(created.get(1).get("amount").decimalValue());
        assertThat(charged).isEqualByComparingTo(quote.get("totalAmount").decimalValue());
    }

    @Test
    void oneWayPreviewHasOneLeg() throws Exception {
        String preview = mvc.perform(get("/api/public/booking-quote?airportId=" + tun + "&zoneId=" + z01
                + "&vehicleTypeId=" + sedan))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode quote = json.readTree(preview);

        assertThat(quote.get("legs").asInt()).isEqualTo(1);
        assertDecimal(quote.get("totalAmount"), "50.000");
    }

    @Test
    void biggerVehicleCanCarryFewerPassengersButNotTheOtherWayRound() throws Exception {
        String token = newCustomerToken();

        ObjectNode tooMany = request();
        tooMany.put("passengers", 4);
        create(token, tooMany).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VEHICLE_CAPACITY_EXCEEDED"));

        ObjectNode vanForFour = request();
        vanForFour.put("passengers", 4);
        vanForFour.put("vehicleTypeId", van);
        create(token, vanForFour).andExpect(status().isCreated());

        ObjectNode vanForTwo = request();
        vanForTwo.put("vehicleTypeId", van);
        create(token, vanForTwo).andExpect(status().isCreated());
    }

    @Test
    void invalidRequestsAreRejectedWithStableCodes() throws Exception {
        String token = newCustomerToken();

        ObjectNode past = request();
        past.put("pickupAt", Instant.now().minus(1, ChronoUnit.HOURS).toString());
        create(token, past).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PICKUP_IN_PAST"));

        ObjectNode returnBefore = request();
        returnBefore.put("returnPickupAt", Instant.now().plus(1, ChronoUnit.DAYS).toString());
        create(token, returnBefore).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("RETURN_BEFORE_OUTBOUND"));

        ObjectNode card = request();
        card.put("paymentMethod", "CARD");
        create(token, card).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_NOT_AVAILABLE"));

        ObjectNode unknownAirport = request();
        unknownAirport.put("airportId", 999_999);
        create(token, unknownAirport).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("AIRPORT_NOT_FOUND"));

        ObjectNode badCurrency = request();
        badCurrency.put("currency", "GBP");
        create(token, badCurrency).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CURRENCY_NOT_SUPPORTED"));

        ObjectNode invalid = request();
        invalid.put("address", " ");
        invalid.put("passengers", 0);
        invalid.put("flightNumber", "!!");
        create(token, invalid).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.address").value("NotBlank"))
            .andExpect(jsonPath("$.fieldErrors.passengers").value("Min"))
            .andExpect(jsonPath("$.fieldErrors.flightNumber").value("Pattern"));
    }

    @Test
    void bookingRequiresAnAccount() throws Exception {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(request().toString()))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
    }

    // ---- reading and cancelling

    @Test
    void customersOnlySeeAndCancelTheirOwnBookings() throws Exception {
        String alice = newCustomerToken();
        String bob = newCustomerToken();
        long id = createOk(alice, request()).get("bookings").get(0).get("id").asLong();

        assertThat(getJson(alice, "/api/bookings")).hasSize(1);
        assertThat(getJson(bob, "/api/bookings")).isEmpty();
        mvc.perform(get("/api/bookings/" + id).header("Authorization", "Bearer " + bob))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
        mvc.perform(post("/api/bookings/" + id + "/cancel").header("Authorization", "Bearer " + bob))
            .andExpect(status().isNotFound());
        assertThat(getJson(alice, "/api/bookings/" + id).get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void myBookingsAreListedLatestPickupFirst() throws Exception {
        String token = newCustomerToken();
        ObjectNode soon = request();
        soon.put("pickupAt", Instant.now().plus(2, ChronoUnit.DAYS).toString());
        ObjectNode later = request();
        later.put("pickupAt", Instant.now().plus(9, ChronoUnit.DAYS).toString());
        createOk(token, soon);
        createOk(token, later);

        JsonNode list = getJson(token, "/api/bookings");

        assertThat(Instant.parse(list.get(0).get("pickupAt").asText()))
            .isAfter(Instant.parse(list.get(1).get("pickupAt").asText()));
    }

    @Test
    void cancellingWorksOnceAndNeverAfterTheTripStarted() throws Exception {
        String token = newCustomerToken();
        long id = createOk(token, request()).get("bookings").get(0).get("id").asLong();

        mvc.perform(post("/api/bookings/" + id + "/cancel").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.cancellable").value(false));
        mvc.perform(post("/api/bookings/" + id + "/cancel").header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        long second = createOk(token, request()).get("bookings").get(0).get("id").asLong();
        bookings.findById(second).orElseThrow().changeStatus(BookingStatus.IN_PROGRESS);
        bookings.flush();
        mvc.perform(post("/api/bookings/" + second + "/cancel").header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());
        assertThat(getJson(token, "/api/bookings/" + second).get("cancellable").asBoolean()).isFalse();
    }

    @Test
    void cancellingOneLegLeavesTheOtherLegOfARoundTrip() throws Exception {
        String token = newCustomerToken();
        ObjectNode req = request();
        req.put("returnPickupAt", Instant.now().plus(6, ChronoUnit.DAYS).toString());
        JsonNode created = createOk(token, req).get("bookings");

        mvc.perform(post("/api/bookings/" + created.get(0).get("id").asLong() + "/cancel")
            .header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        assertThat(getJson(token, "/api/bookings/" + created.get(1).get("id").asLong()).get("status").asText())
            .isEqualTo("PENDING");
    }
}
