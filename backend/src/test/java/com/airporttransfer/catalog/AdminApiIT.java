package com.airporttransfer.catalog;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin CRUD, authorization and the error format. Test methods run in a transaction rolled back at the end,
 * so a request that ends in a database error (409) must be the last call of its test.
 */
@Transactional
class AdminApiIT extends AbstractIT {

    private static final String NEW_AIRPORT = """
        {"iataCode":"XYZ","nameFr":"Test","nameEn":"Test","nameAr":"اختبار","latitude":35.5,"longitude":10.5,"active":true}
        """;

    @Autowired MockMvc mvc;
    @Autowired AirportRepository airports;
    @Autowired ZoneRepository zones;
    @Autowired VehicleTypeRepository vehicleTypes;

    // ------------------------------------------------------------------ authorization

    @Test
    void anonymousCannotUseAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/airports")).andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerIsForbidden() throws Exception {
        mvc.perform(get("/api/admin/airports")).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/airports").contentType(MediaType.APPLICATION_JSON).content(NEW_AIRPORT))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverIsForbidden() throws Exception {
        mvc.perform(get("/api/admin/tariffs")).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ airports

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminListsAirportsIncludingInactiveOnes() throws Exception {
        mvc.perform(get("/api/admin/airports"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreatesUpdatesAndDeletesAnAirport() throws Exception {
        mvc.perform(post("/api/admin/airports").contentType(MediaType.APPLICATION_JSON).content(NEW_AIRPORT))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.iataCode").value("XYZ"));

        Long id = airports.findAll().stream().filter(a -> a.getIataCode().equals("XYZ")).findFirst().orElseThrow().getId();

        mvc.perform(put("/api/admin/airports/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(NEW_AIRPORT.replace("\"active\":true", "\"active\":false").replace("\"Test\"", "\"Renamed\"")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nameFr").value("Renamed"))
            .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/public/airports")).andExpect(jsonPath("$.length()").value(8)); // inactive: hidden

        mvc.perform(delete("/api/admin/airports/" + id)).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidAirportReturnsFieldErrorsInTheStandardFormat() throws Exception {
        mvc.perform(post("/api/admin/airports").contentType(MediaType.APPLICATION_JSON)
                .content("{\"iataCode\":\"xy\",\"nameFr\":\"\",\"nameEn\":\"A\",\"nameAr\":\"A\",\"active\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.iataCode").value("Pattern"))
            .andExpect(jsonPath("$.fieldErrors.nameFr").value("NotBlank"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatingAnUnknownAirportIsA404() throws Exception {
        mvc.perform(put("/api/admin/airports/999999").contentType(MediaType.APPLICATION_JSON).content(NEW_AIRPORT))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("AIRPORT_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingAnAirportThatHasTariffsIsA409() throws Exception {
        Long tun = airports.findAll().stream().filter(a -> a.getIataCode().equals("TUN")).findFirst().orElseThrow().getId();

        mvc.perform(delete("/api/admin/airports/" + tun))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateAirportCodeIsA409() throws Exception {
        mvc.perform(post("/api/admin/airports").contentType(MediaType.APPLICATION_JSON)
                .content(NEW_AIRPORT.replace("XYZ", "TUN")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    // ------------------------------------------------------------------ zones and areas

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminMovesAnAreaToAnotherZone() throws Exception {
        Long z02 = zones.findAll().stream().filter(z -> z.getCode().equals("Z02")).findFirst().orElseThrow().getId();
        Long manouba = findAreaId("TN-14");

        mvc.perform(put("/api/admin/areas/" + manouba).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"TN-14\",\"nameFr\":\"La Manouba\",\"nameEn\":\"Manouba\",\"nameAr\":\"منوبة\","
                    + "\"zoneId\":" + z02 + ",\"active\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.zoneId").value(z02));

        mvc.perform(get("/api/public/zones"))
            .andExpect(jsonPath("$[0].areas.length()").value(3))  // Z01 lost Manouba
            .andExpect(jsonPath("$[1].areas.length()").value(2)); // Z02 now has Nabeul + Manouba
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingAnAreaInAnUnknownZoneIsA404() throws Exception {
        mvc.perform(post("/api/admin/areas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"X-1\",\"nameFr\":\"X\",\"nameEn\":\"X\",\"nameAr\":\"X\",\"zoneId\":999999,\"active\":true}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ZONE_NOT_FOUND"));
    }

    // ------------------------------------------------------------------ vehicle types

    @Test
    @WithMockUser(roles = "ADMIN")
    void vehicleTypeCapacityMustBeConsistent() throws Exception {
        mvc.perform(post("/api/admin/vehicle-types").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"LIMO\",\"nameFr\":\"L\",\"nameEn\":\"L\",\"nameAr\":\"L\","
                    + "\"minPassengers\":5,\"maxPassengers\":2,\"displayOrder\":9,\"active\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_CAPACITY"));
    }

    // ------------------------------------------------------------------ tariffs, currencies, exchange rates

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminTariffIsRealAndAppliesToQuotes() throws Exception {
        Long tun = airports.findAll().stream().filter(a -> a.getIataCode().equals("TUN")).findFirst().orElseThrow().getId();
        Long z01 = zones.findAll().stream().filter(z -> z.getCode().equals("Z01")).findFirst().orElseThrow().getId();
        Long sedan = vehicleTypes.findAll().stream().filter(v -> v.getCode().equals("SEDAN")).findFirst().orElseThrow().getId();
        Long tariffId = findTariffId(tun, z01, sedan);

        mvc.perform(put("/api/admin/tariffs/" + tariffId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"airportId\":" + tun + ",\"zoneId\":" + z01 + ",\"vehicleTypeId\":" + sedan
                    + ",\"priceTnd\":65.500,\"active\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priceTnd").value(comparesEqualTo(65.5)))
            .andExpect(jsonPath("$.demo").value(false));

        mvc.perform(get("/api/public/quote").param("airportId", "" + tun).param("zoneId", "" + z01)
                .param("vehicleTypeId", "" + sedan))
            .andExpect(jsonPath("$.priceTnd").value(comparesEqualTo(65.5)))
            .andExpect(jsonPath("$.demo").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateTariffRouteIsA409() throws Exception {
        Long tun = airports.findAll().stream().filter(a -> a.getIataCode().equals("TUN")).findFirst().orElseThrow().getId();
        Long z01 = zones.findAll().stream().filter(z -> z.getCode().equals("Z01")).findFirst().orElseThrow().getId();
        Long sedan = vehicleTypes.findAll().stream().filter(v -> v.getCode().equals("SEDAN")).findFirst().orElseThrow().getId();

        mvc.perform(post("/api/admin/tariffs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"airportId\":" + tun + ",\"zoneId\":" + z01 + ",\"vehicleTypeId\":" + sedan
                    + ",\"priceTnd\":99,\"active\":true}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void nonPositiveTariffPriceIsRejected() throws Exception {
        mvc.perform(post("/api/admin/tariffs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"airportId\":1,\"zoneId\":1,\"vehicleTypeId\":1,\"priceTnd\":0,\"active\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.priceTnd").value("DecimalMin"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void tndCannotBeDeactivated() throws Exception {
        mvc.perform(put("/api/admin/currencies/TND").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"TND\",\"name\":\"Dinar tunisien\",\"symbol\":\"DT\",\"decimals\":3,\"active\":false}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BASE_CURRENCY_LOCKED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void tndHasNoExchangeRate() throws Exception {
        mvc.perform(post("/api/admin/exchange-rates").contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"TND\",\"tndPerUnit\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BASE_CURRENCY_NO_RATE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newExchangeRateIsAddedToTheHistory() throws Exception {
        mvc.perform(get("/api/admin/exchange-rates").param("currency", "USD"))
            .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(post("/api/admin/exchange-rates").contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"USD\",\"tndPerUnit\":3.2}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.demo").value(false));

        mvc.perform(get("/api/admin/exchange-rates").param("currency", "USD"))
            .andExpect(jsonPath("$.length()").value(2))   // the demo rate is kept as history
            .andExpect(jsonPath("$[?(@.demo==false)].tndPerUnit").value(comparesEqualTo(3.2)));
    }

    // ------------------------------------------------------------------ helpers

    private Long findAreaId(String code) {
        return findIdByCode("areas", code);
    }

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    private Long findIdByCode(String table, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE code = ?", Long.class, code);
    }

    private Long findTariffId(Long airportId, Long zoneId, Long vehicleTypeId) {
        return jdbc.queryForObject(
            "SELECT id FROM tariffs WHERE airport_id = ? AND zone_id = ? AND vehicle_type_id = ?",
            Long.class, airportId, zoneId, vehicleTypeId);
    }
}
