package com.airporttransfer.pricing;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.AbstractIT;
import com.airporttransfer.catalog.AirportRepository;
import com.airporttransfer.catalog.VehicleTypeRepository;
import com.airporttransfer.catalog.ZoneRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** GET /api/public/quote against the seeded demo data (the test transaction is rolled back). */
@Transactional
class QuoteIT extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired AirportRepository airports;
    @Autowired ZoneRepository zones;
    @Autowired VehicleTypeRepository vehicleTypes;
    @Autowired ExchangeRateRepository rates;

    Long tun;
    Long z01;
    Long sedan;

    @BeforeEach
    void ids() {
        tun = airports.findAll().stream().filter(a -> a.getIataCode().equals("TUN")).findFirst().orElseThrow().getId();
        z01 = zones.findAll().stream().filter(z -> z.getCode().equals("Z01")).findFirst().orElseThrow().getId();
        sedan = vehicleTypes.findAll().stream().filter(v -> v.getCode().equals("SEDAN")).findFirst().orElseThrow().getId();
    }

    /** Replaces every EUR rate, so the test does not depend on timestamps (DB clock vs JVM clock). */
    private void onlyEurRate(String tndPerEuro) {
        rates.deleteAll(rates.findByCurrencyCodeOrderByCreatedAtDescIdDesc("EUR"));
        rates.flush();
        ExchangeRate r = new ExchangeRate();
        r.setCurrencyCode("EUR");
        r.setTndPerUnit(new BigDecimal(tndPerEuro));
        r.setDemo(false);
        rates.saveAndFlush(r);
    }

    @Test
    void tunisToGrandTunisSedanIsTheDemoBasePrice() throws Exception {
        mvc.perform(get("/api/public/quote").param("airportId", "" + tun).param("zoneId", "" + z01)
                .param("vehicleTypeId", "" + sedan))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currency").value("TND"))
            .andExpect(jsonPath("$.priceTnd").value(comparesEqualTo(50.0)))
            .andExpect(jsonPath("$.exchangeRate").doesNotExist())
            .andExpect(jsonPath("$.demo").value(true));
    }

    @Test
    void convertsWithTheLatestExchangeRate() throws Exception {
        onlyEurRate("2.500000");

        mvc.perform(get("/api/public/quote").param("airportId", "" + tun).param("zoneId", "" + z01)
                .param("vehicleTypeId", "" + sedan).param("currency", "EUR"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(20.0))) // 50 TND / 2.5
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(2.5)))
            .andExpect(jsonPath("$.demo").value(true)); // the tariff is still a placeholder
    }

    @Test
    void unsupportedCurrencyIsABadRequestWithAStableCode() throws Exception {
        mvc.perform(get("/api/public/quote").param("airportId", "" + tun).param("zoneId", "" + z01)
                .param("vehicleTypeId", "" + sedan).param("currency", "GBP"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CURRENCY_NOT_SUPPORTED"));
    }

    @Test
    void unknownAirportIsA404WithAStableCode() throws Exception {
        mvc.perform(get("/api/public/quote").param("airportId", "999999").param("zoneId", "" + z01)
                .param("vehicleTypeId", "" + sedan))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("AIRPORT_NOT_FOUND"));
    }

    @Test
    void missingParameterIsABadRequest() throws Exception {
        mvc.perform(get("/api/public/quote").param("airportId", "" + tun))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
