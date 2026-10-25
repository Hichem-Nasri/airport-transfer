package com.airporttransfer.pricing;

import com.airporttransfer.catalog.Airport;
import com.airporttransfer.catalog.AirportRepository;
import com.airporttransfer.catalog.VehicleType;
import com.airporttransfer.catalog.VehicleTypeRepository;
import com.airporttransfer.catalog.Zone;
import com.airporttransfer.catalog.ZoneRepository;
import com.airporttransfer.common.ApiException;
import com.airporttransfer.domain.Currency;
import com.airporttransfer.pricing.PricingDtos.CurrencyDto;
import com.airporttransfer.pricing.PricingDtos.CurrencyRequest;
import com.airporttransfer.pricing.PricingDtos.ExchangeRateDto;
import com.airporttransfer.pricing.PricingDtos.ExchangeRateRequest;
import com.airporttransfer.pricing.PricingDtos.TariffDto;
import com.airporttransfer.pricing.PricingDtos.TariffRequest;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin CRUD for currencies, exchange rates and tariffs, plus the public list of active currencies. */
@Service
public class PricingAdminService {

    private static final String BASE = Currency.TND.name();

    private final CurrencyRepository currencies;
    private final ExchangeRateRepository rates;
    private final TariffRepository tariffs;
    private final AirportRepository airports;
    private final ZoneRepository zones;
    private final VehicleTypeRepository vehicleTypes;
    private final PricingMapper mapper;

    public PricingAdminService(CurrencyRepository currencies, ExchangeRateRepository rates, TariffRepository tariffs,
                               AirportRepository airports, ZoneRepository zones, VehicleTypeRepository vehicleTypes,
                               PricingMapper mapper) {
        this.currencies = currencies;
        this.rates = rates;
        this.tariffs = tariffs;
        this.airports = airports;
        this.zones = zones;
        this.vehicleTypes = vehicleTypes;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- currencies

    @Transactional(readOnly = true)
    public List<CurrencyDto> listActiveCurrencies() {
        return currencies.findAllByActiveTrueOrderByCodeAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CurrencyDto> listCurrencies() {
        return currencies.findAllByOrderByCodeAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public CurrencyDto createCurrency(CurrencyRequest request) {
        if (currencies.existsById(request.code())) {
            throw ApiException.conflict("CURRENCY_ALREADY_EXISTS", "Currency already exists: " + request.code());
        }
        CurrencyDefinition currency = new CurrencyDefinition();
        currency.setCode(request.code());
        mapper.update(request, currency);
        return mapper.toDto(currencies.saveAndFlush(currency));
    }

    @Transactional
    public CurrencyDto updateCurrency(String code, CurrencyRequest request) {
        if (!code.equals(request.code())) {
            throw ApiException.badRequest("CURRENCY_CODE_MISMATCH", "The currency code cannot be changed");
        }
        if (BASE.equals(code) && !Boolean.TRUE.equals(request.active())) {
            throw ApiException.badRequest("BASE_CURRENCY_LOCKED", "TND is the base currency and must stay active");
        }
        CurrencyDefinition currency = currencies.findById(code)
            .orElseThrow(() -> ApiException.notFound("CURRENCY_NOT_FOUND", "Unknown currency: " + code));
        mapper.update(request, currency);
        return mapper.toDto(currencies.saveAndFlush(currency));
    }

    // ---------------------------------------------------------------- exchange rates

    @Transactional(readOnly = true)
    public List<ExchangeRateDto> listRates(String currencyCode) {
        List<ExchangeRate> found = currencyCode == null || currencyCode.isBlank()
            ? rates.findAllByOrderByCreatedAtDescIdDesc()
            : rates.findByCurrencyCodeOrderByCreatedAtDescIdDesc(currencyCode);
        return found.stream().map(mapper::toDto).toList();
    }

    /** Rates are history: a new rate is a new row, and the most recent one applies to new quotes. */
    @Transactional
    public ExchangeRateDto createRate(ExchangeRateRequest request) {
        if (BASE.equals(request.currencyCode())) {
            throw ApiException.badRequest("BASE_CURRENCY_NO_RATE", "TND is the base currency: it has no exchange rate");
        }
        if (!currencies.existsById(request.currencyCode())) {
            throw ApiException.notFound("CURRENCY_NOT_FOUND", "Unknown currency: " + request.currencyCode());
        }
        ExchangeRate rate = new ExchangeRate();
        rate.setCurrencyCode(request.currencyCode());
        rate.setTndPerUnit(request.tndPerUnit());
        rate.setDemo(false);
        return mapper.toDto(rates.saveAndFlush(rate));
    }

    // ---------------------------------------------------------------- tariffs

    @Transactional(readOnly = true)
    public List<TariffDto> listTariffs(Long airportId, Long zoneId, Long vehicleTypeId) {
        return tariffs.findAll().stream()
            .filter(t -> airportId == null || t.getAirport().getId().equals(airportId))
            .filter(t -> zoneId == null || t.getZone().getId().equals(zoneId))
            .filter(t -> vehicleTypeId == null || t.getVehicleType().getId().equals(vehicleTypeId))
            .sorted(Comparator.comparing(Tariff::getId))
            .map(mapper::toDto)
            .toList();
    }

    @Transactional
    public TariffDto createTariff(TariffRequest request) {
        Tariff tariff = new Tariff();
        apply(tariff, request);
        return mapper.toDto(tariffs.saveAndFlush(tariff));
    }

    @Transactional
    public TariffDto updateTariff(Long id, TariffRequest request) {
        Tariff tariff = tariffs.findById(id)
            .orElseThrow(() -> ApiException.notFound("TARIFF_NOT_FOUND", "Unknown tariff: " + id));
        apply(tariff, request);
        return mapper.toDto(tariffs.saveAndFlush(tariff));
    }

    @Transactional
    public void deleteTariff(Long id) {
        Tariff tariff = tariffs.findById(id)
            .orElseThrow(() -> ApiException.notFound("TARIFF_NOT_FOUND", "Unknown tariff: " + id));
        tariffs.delete(tariff);
        tariffs.flush();
    }

    /** A tariff saved through the admin API is a real one (demo = false), whatever it was before. */
    private void apply(Tariff tariff, TariffRequest request) {
        Airport airport = airports.findById(request.airportId())
            .orElseThrow(() -> ApiException.notFound("AIRPORT_NOT_FOUND", "Unknown airport: " + request.airportId()));
        Zone zone = zones.findById(request.zoneId())
            .orElseThrow(() -> ApiException.notFound("ZONE_NOT_FOUND", "Unknown zone: " + request.zoneId()));
        VehicleType vehicleType = vehicleTypes.findById(request.vehicleTypeId())
            .orElseThrow(() -> ApiException.notFound("VEHICLE_TYPE_NOT_FOUND",
                "Unknown vehicle type: " + request.vehicleTypeId()));
        mapper.update(request, tariff);
        tariff.setAirport(airport);
        tariff.setZone(zone);
        tariff.setVehicleType(vehicleType);
        tariff.setPriceTnd(request.priceTnd().setScale(3, RoundingMode.HALF_UP));
        tariff.setDemo(false);
    }
}
