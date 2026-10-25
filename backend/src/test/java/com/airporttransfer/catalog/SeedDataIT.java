package com.airporttransfer.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.airporttransfer.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** The Flyway migrations apply on an empty database and leave the expected reference data. */
class SeedDataIT extends AbstractIT {

    @Autowired
    JdbcTemplate jdbc;

    private int count(String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }

    @Test
    void seedsTheEightAirportsThreeVehicleTypesAndThreeCurrencies() {
        assertThat(count("SELECT count(*) FROM airports")).isEqualTo(8);
        assertThat(count("SELECT count(*) FROM airports WHERE iata_code IN ('TUN','MIR','NBE','DJE','SFA','TOE','TBJ','GAF')"))
            .isEqualTo(8);
        assertThat(count("SELECT count(*) FROM vehicle_types")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM currencies WHERE code IN ('TND','EUR','USD')")).isEqualTo(3);
    }

    @Test
    void vehicleCapacitiesFollowTheSpecification() {
        assertThat(count("SELECT count(*) FROM vehicle_types WHERE code='SEDAN' AND min_passengers=1 AND max_passengers=3"))
            .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM vehicle_types WHERE code='VAN' AND min_passengers=4 AND max_passengers=7"))
            .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM vehicle_types WHERE code='MINIBUS' AND min_passengers=8 AND max_passengers=16"))
            .isEqualTo(1);
    }

    @Test
    void coversAllGovernoratesWithMedenineSplitIntoDjerbaAndMainland() {
        assertThat(count("SELECT count(*) FROM zones")).isEqualTo(15);
        assertThat(count("SELECT count(*) FROM areas")).isEqualTo(25); // 24 governorates, Médenine counted twice
        assertThat(count("SELECT count(*) FROM areas WHERE code LIKE 'TN-82-%'")).isEqualTo(2);
        assertThat(count("""
            SELECT count(*) FROM areas a1 JOIN areas a2 ON a1.code = 'TN-82-DJERBA' AND a2.code = 'TN-82-MAIN'
            WHERE a1.zone_id <> a2.zone_id
            """)).isEqualTo(1);
    }

    @Test
    void everyZoneHasAtLeastOneAreaAndAllNamesAreTranslated() {
        assertThat(count("SELECT count(*) FROM zones z WHERE NOT EXISTS (SELECT 1 FROM areas a WHERE a.zone_id = z.id)"))
            .isZero();
        assertThat(count("SELECT count(*) FROM zones WHERE name_fr = '' OR name_en = '' OR name_ar = ''")).isZero();
        assertThat(count("SELECT count(*) FROM airports WHERE name_fr = '' OR name_en = '' OR name_ar = ''")).isZero();
    }

    @Test
    void everyAirportZoneVehicleCombinationHasExactlyOneDemoTariff() {
        assertThat(count("SELECT count(*) FROM tariffs")).isEqualTo(8 * 15 * 3);
        assertThat(count("SELECT count(*) FROM tariffs WHERE is_demo = FALSE")).isZero();
    }

    @Test
    void demoPricesGrowWithTheVehicleSize() {
        assertThat(count("""
            SELECT count(*) FROM tariffs s
            JOIN vehicle_types sv ON sv.id = s.vehicle_type_id AND sv.code = 'SEDAN'
            JOIN tariffs m ON m.airport_id = s.airport_id AND m.zone_id = s.zone_id
            JOIN vehicle_types mv ON mv.id = m.vehicle_type_id AND mv.code = 'MINIBUS'
            WHERE m.price_tnd <= s.price_tnd
            """)).isZero();
    }

    @Test
    void seedsOneDemoRateForEurAndUsdAndNoneForTnd() {
        assertThat(count("SELECT count(*) FROM exchange_rates WHERE is_demo AND currency_code IN ('EUR','USD')"))
            .isEqualTo(2);
        assertThat(count("SELECT count(*) FROM exchange_rates WHERE currency_code = 'TND'")).isZero();
    }
}
