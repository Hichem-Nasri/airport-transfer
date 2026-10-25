package com.airporttransfer.pricing;

import com.airporttransfer.pricing.PricingDtos.CurrencyDto;
import com.airporttransfer.pricing.PricingDtos.QuoteDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicPricingController {

    private final QuoteService quoteService;
    private final PricingAdminService pricingService;

    public PublicPricingController(QuoteService quoteService, PricingAdminService pricingService) {
        this.quoteService = quoteService;
        this.pricingService = pricingService;
    }

    /** Active currencies, to build the currency switch. */
    @GetMapping("/currencies")
    public List<CurrencyDto> currencies() {
        return pricingService.listActiveCurrencies();
    }

    @GetMapping("/quote")
    public QuoteDto quote(@RequestParam("airportId") Long airportId,
                          @RequestParam("zoneId") Long zoneId,
                          @RequestParam("vehicleTypeId") Long vehicleTypeId,
                          @RequestParam(name = "currency", defaultValue = "TND") String currency) {
        return quoteService.quote(airportId, zoneId, vehicleTypeId, currency);
    }
}
