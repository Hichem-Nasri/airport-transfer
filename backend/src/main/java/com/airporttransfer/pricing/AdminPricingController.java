package com.airporttransfer.pricing;

import com.airporttransfer.pricing.PricingDtos.CurrencyDto;
import com.airporttransfer.pricing.PricingDtos.CurrencyRequest;
import com.airporttransfer.pricing.PricingDtos.ExchangeRateDto;
import com.airporttransfer.pricing.PricingDtos.ExchangeRateRequest;
import com.airporttransfer.pricing.PricingDtos.TariffDto;
import com.airporttransfer.pricing.PricingDtos.TariffRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin CRUD for currencies, exchange rates and tariffs. Restricted to ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminPricingController {

    private final PricingAdminService service;

    public AdminPricingController(PricingAdminService service) {
        this.service = service;
    }

    // ---- currencies
    @GetMapping("/currencies")
    public List<CurrencyDto> currencies() {
        return service.listCurrencies();
    }

    @PostMapping("/currencies")
    @ResponseStatus(HttpStatus.CREATED)
    public CurrencyDto createCurrency(@Valid @RequestBody CurrencyRequest request) {
        return service.createCurrency(request);
    }

    @PutMapping("/currencies/{code}")
    public CurrencyDto updateCurrency(@PathVariable String code, @Valid @RequestBody CurrencyRequest request) {
        return service.updateCurrency(code, request);
    }

    // ---- exchange rates
    @GetMapping("/exchange-rates")
    public List<ExchangeRateDto> exchangeRates(@RequestParam(name = "currency", required = false) String currency) {
        return service.listRates(currency);
    }

    @PostMapping("/exchange-rates")
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRateDto createExchangeRate(@Valid @RequestBody ExchangeRateRequest request) {
        return service.createRate(request);
    }

    // ---- tariffs
    @GetMapping("/tariffs")
    public List<TariffDto> tariffs(@RequestParam(name = "airportId", required = false) Long airportId,
                                   @RequestParam(name = "zoneId", required = false) Long zoneId,
                                   @RequestParam(name = "vehicleTypeId", required = false) Long vehicleTypeId) {
        return service.listTariffs(airportId, zoneId, vehicleTypeId);
    }

    @PostMapping("/tariffs")
    @ResponseStatus(HttpStatus.CREATED)
    public TariffDto createTariff(@Valid @RequestBody TariffRequest request) {
        return service.createTariff(request);
    }

    @PutMapping("/tariffs/{id}")
    public TariffDto updateTariff(@PathVariable Long id, @Valid @RequestBody TariffRequest request) {
        return service.updateTariff(id, request);
    }

    @DeleteMapping("/tariffs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTariff(@PathVariable Long id) {
        service.deleteTariff(id);
    }
}
