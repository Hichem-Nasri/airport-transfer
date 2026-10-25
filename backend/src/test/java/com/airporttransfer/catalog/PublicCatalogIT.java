package com.airporttransfer.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** The read endpoints used by the booking form are public (no token). */
class PublicCatalogIT extends AbstractIT {

    @Autowired
    MockMvc mvc;

    @Test
    void listsTheActiveAirports() throws Exception {
        mvc.perform(get("/api/public/airports"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(8))
            .andExpect(jsonPath("$[0].iataCode").value("DJE")) // sorted by IATA code
            .andExpect(jsonPath("$[0].nameAr").isNotEmpty());
    }

    @Test
    void listsZonesWithTheirAreas() throws Exception {
        mvc.perform(get("/api/public/zones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(15))
            .andExpect(jsonPath("$[0].code").value("Z01"))
            .andExpect(jsonPath("$[0].areas.length()").value(4)); // Tunis, Ariana, Ben Arous, Manouba
    }

    @Test
    void listsTheVehicleTypesInDisplayOrder() throws Exception {
        mvc.perform(get("/api/public/vehicle-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].code").value("SEDAN"))
            .andExpect(jsonPath("$[2].code").value("MINIBUS"))
            .andExpect(jsonPath("$[2].maxPassengers").value(16));
    }

    @Test
    void listsTheActiveCurrencies() throws Exception {
        mvc.perform(get("/api/public/currencies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[?(@.code=='TND')].decimals").value(3))
            .andExpect(jsonPath("$[?(@.code=='EUR')].decimals").value(2));
    }
}
