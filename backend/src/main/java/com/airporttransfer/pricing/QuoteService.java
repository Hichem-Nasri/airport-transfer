package com.airporttransfer.pricing;

import com.airporttransfer.catalog.AirportRepository;
import com.airporttransfer.catalog.VehicleTypeRepository;
import com.airporttransfer.catalog.ZoneRepository;
import com.airporttransfer.common.ApiException;
import com.airporttransfer.domain.Currency;
import com.airporttransfer.pricing.PricingDtos.QuoteDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flat-rate pricing: one price in TND per (airport, destination zone, vehicle type), converted to the
 * requested currency with the latest exchange rate (TND per 1 unit). Rounding is HALF_UP, to the number of
 * decimals of the target currency, and only happens here, at conversion time.
 */
@Service
public class QuoteService {

    private static final String BASE = Currency.TND.name();

    private final AirportRepository airports;
    private final ZoneRepository zones;
    private final VehicleTypeRepository vehicleTypes;
    private final CurrencyRepository currencies;
    private final TariffRepository tariffs;
    private final ExchangeRateRepository rates;

    public QuoteService(AirportRepository airports, ZoneRepository zones, VehicleTypeRepository vehicleTypes,
                        CurrencyRepository currencies, TariffRepository tariffs, ExchangeRateRepository rates) {
        this.airports = airports;
        this.zones = zones;
        this.vehicleTypes = vehicleTypes;
        this.currencies = currencies;
        this.tariffs = tariffs;
        this.rates = rates;
    }

    @Transactional(readOnly = true)
    public QuoteDto quote(Long airportId, Long zoneId, Long vehicleTypeId, String currencyCode) {
        String code = currencyCode == null || currencyCode.isBlank() ? BASE : currencyCode.trim().toUpperCase(Locale.ROOT);

        airports.findByIdAndActiveTrue(airportId)
            .orElseThrow(() -> ApiException.notFound("AIRPORT_NOT_FOUND", "Unknown or inactive airport: " + airportId));
        zones.findByIdAndActiveTrue(zoneId)
            .orElseThrow(() -> ApiException.notFound("ZONE_NOT_FOUND", "Unknown or inactive zone: " + zoneId));
        vehicleTypes.findByIdAndActiveTrue(vehicleTypeId)
            .orElseThrow(() -> ApiException.notFound("VEHICLE_TYPE_NOT_FOUND",
                "Unknown or inactive vehicle type: " + vehicleTypeId));
        CurrencyDefinition currency = currencies.findByCodeAndActiveTrue(code)
            .orElseThrow(() -> ApiException.badRequest("CURRENCY_NOT_SUPPORTED", "Unsupported currency: " + code));
        Tariff tariff = tariffs.findByAirportIdAndZoneIdAndVehicleTypeIdAndActiveTrue(airportId, zoneId, vehicleTypeId)
            .orElseThrow(() -> ApiException.notFound("TARIFF_NOT_FOUND", "No tariff for this airport, zone and vehicle"));

        BigDecimal priceTnd = tariff.getPriceTnd().setScale(3, RoundingMode.HALF_UP);
        if (BASE.equals(code)) {
            return new QuoteDto(airportId, zoneId, vehicleTypeId, priceTnd, BASE,
                priceTnd.setScale(currency.getDecimals(), RoundingMode.HALF_UP), null, tariff.isDemo());
        }

        ExchangeRate rate = rates.findFirstByCurrencyCodeOrderByCreatedAtDescIdDesc(code)
            .orElseThrow(() -> ApiException.notFound("EXCHANGE_RATE_NOT_FOUND", "No exchange rate for " + code));
        BigDecimal amount = priceTnd.divide(rate.getTndPerUnit(), currency.getDecimals(), RoundingMode.HALF_UP);
        return new QuoteDto(airportId, zoneId, vehicleTypeId, priceTnd, code, amount, rate.getTndPerUnit(),
            tariff.isDemo() || rate.isDemo());
    }
}
