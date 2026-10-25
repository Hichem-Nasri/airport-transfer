package com.airporttransfer.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.airporttransfer.catalog.Airport;
import com.airporttransfer.catalog.AirportRepository;
import com.airporttransfer.catalog.VehicleType;
import com.airporttransfer.catalog.VehicleTypeRepository;
import com.airporttransfer.catalog.Zone;
import com.airporttransfer.catalog.ZoneRepository;
import com.airporttransfer.common.ApiException;
import com.airporttransfer.pricing.PricingDtos.QuoteDto;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Money rules of the quote, without a database: conversion direction, rounding, demo flag, errors. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteServiceTest {

    @Mock AirportRepository airports;
    @Mock ZoneRepository zones;
    @Mock VehicleTypeRepository vehicleTypes;
    @Mock CurrencyRepository currencies;
    @Mock TariffRepository tariffs;
    @Mock ExchangeRateRepository rates;

    QuoteService service;

    @BeforeEach
    void setUp() {
        service = new QuoteService(airports, zones, vehicleTypes, currencies, tariffs, rates);
        when(airports.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(new Airport()));
        when(zones.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(new Zone()));
        when(vehicleTypes.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(new VehicleType()));
        when(currencies.findByCodeAndActiveTrue("TND")).thenReturn(Optional.of(currency("TND", 3)));
        when(currencies.findByCodeAndActiveTrue("EUR")).thenReturn(Optional.of(currency("EUR", 2)));
        when(currencies.findByCodeAndActiveTrue("USD")).thenReturn(Optional.of(currency("USD", 2)));
    }

    private static CurrencyDefinition currency(String code, int decimals) {
        CurrencyDefinition c = new CurrencyDefinition();
        c.setCode(code);
        c.setDecimals(decimals);
        c.setActive(true);
        return c;
    }

    private void givenTariff(String priceTnd, boolean demo) {
        Tariff t = new Tariff();
        t.setPriceTnd(new BigDecimal(priceTnd));
        t.setDemo(demo);
        t.setActive(true);
        when(tariffs.findByAirportIdAndZoneIdAndVehicleTypeIdAndActiveTrue(1L, 2L, 3L)).thenReturn(Optional.of(t));
    }

    private void givenRate(String currency, String tndPerUnit, boolean demo) {
        ExchangeRate r = new ExchangeRate();
        r.setCurrencyCode(currency);
        r.setTndPerUnit(new BigDecimal(tndPerUnit));
        r.setDemo(demo);
        when(rates.findFirstByCurrencyCodeOrderByCreatedAtDescIdDesc(currency)).thenReturn(Optional.of(r));
    }

    @Test
    void tndQuoteIsTheBasePriceWithThreeDecimalsAndNoRate() {
        givenTariff("120", false);

        QuoteDto q = service.quote(1L, 2L, 3L, "TND");

        assertThat(q.currency()).isEqualTo("TND");
        assertThat(q.priceTnd()).isEqualTo(new BigDecimal("120.000"));
        assertThat(q.amount()).isEqualTo(new BigDecimal("120.000"));
        assertThat(q.exchangeRate()).isNull();
        assertThat(q.demo()).isFalse();
    }

    @Test
    void convertingToEurDividesTheTndPriceByTheTndPerEuroRate() {
        givenTariff("170", false);
        givenRate("EUR", "3.400000", false);

        QuoteDto q = service.quote(1L, 2L, 3L, "EUR");

        assertThat(q.amount()).isEqualTo(new BigDecimal("50.00")); // 170 TND / 3.4 TND per EUR
        assertThat(q.exchangeRate()).isEqualByComparingTo("3.4");
        assertThat(q.priceTnd()).isEqualTo(new BigDecimal("170.000"));
    }

    @Test
    void roundsHalfUpToTheCurrencyDecimals() {
        givenTariff("10", false);
        givenRate("USD", "3.000000", false);
        assertThat(service.quote(1L, 2L, 3L, "USD").amount()).isEqualTo(new BigDecimal("3.33")); // 3.3333...

        givenTariff("10", false);
        givenRate("USD", "4.000000", false);
        assertThat(service.quote(1L, 2L, 3L, "USD").amount()).isEqualTo(new BigDecimal("2.50"));

        givenTariff("0.105", false);
        givenRate("EUR", "1.000000", false);
        assertThat(service.quote(1L, 2L, 3L, "EUR").amount()).isEqualTo(new BigDecimal("0.11")); // 0.105 -> HALF_UP
    }

    @Test
    void currencyCodeIsCaseInsensitive() {
        givenTariff("170", false);
        givenRate("EUR", "3.400000", false);

        assertThat(service.quote(1L, 2L, 3L, " eur ").currency()).isEqualTo("EUR");
    }

    @Test
    void missingCurrencyDefaultsToTnd() {
        givenTariff("90", false);

        assertThat(service.quote(1L, 2L, 3L, null).currency()).isEqualTo("TND");
    }

    @Test
    void quoteIsDemoWhenTheTariffOrTheRateIsAPlaceholder() {
        givenTariff("100", true);
        givenRate("EUR", "3.400000", false);
        assertThat(service.quote(1L, 2L, 3L, "EUR").demo()).isTrue();

        givenTariff("100", false);
        givenRate("EUR", "3.400000", true);
        assertThat(service.quote(1L, 2L, 3L, "EUR").demo()).isTrue();

        givenTariff("100", false);
        givenRate("EUR", "3.400000", false);
        assertThat(service.quote(1L, 2L, 3L, "EUR").demo()).isFalse();
    }

    @Test
    void failsWithAStableCodeWhenSomethingIsMissing() {
        givenTariff("100", false);

        assertThatThrownBy(() -> service.quote(99L, 2L, 3L, "TND"))
            .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("AIRPORT_NOT_FOUND"));
        assertThatThrownBy(() -> service.quote(1L, 99L, 3L, "TND"))
            .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("ZONE_NOT_FOUND"));
        assertThatThrownBy(() -> service.quote(1L, 2L, 99L, "TND"))
            .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("VEHICLE_TYPE_NOT_FOUND"));
        assertThatThrownBy(() -> service.quote(1L, 2L, 3L, "GBP"))
            .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("CURRENCY_NOT_SUPPORTED"));
        assertThatThrownBy(() -> service.quote(1L, 2L, 3L, "EUR")) // no exchange rate stubbed for EUR
            .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("EXCHANGE_RATE_NOT_FOUND"));
    }

    @Test
    void failsWhenThereIsNoActiveTariff() {
        when(tariffs.findByAirportIdAndZoneIdAndVehicleTypeIdAndActiveTrue(1L, 2L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.quote(1L, 2L, 3L, "TND"))
            .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("TARIFF_NOT_FOUND"));
    }
}
