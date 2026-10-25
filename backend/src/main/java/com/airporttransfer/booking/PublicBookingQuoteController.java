package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.BookingQuoteDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicBookingQuoteController {

    private final BookingPricingService pricing;

    public PublicBookingQuoteController(BookingPricingService pricing) {
        this.pricing = pricing;
    }

    /** Price preview of a booking: per leg and total, already converted. {@code roundTrip=true} means two legs. */
    @GetMapping("/booking-quote")
    public BookingQuoteDto quote(@RequestParam Long airportId, @RequestParam Long zoneId,
                                 @RequestParam Long vehicleTypeId, @RequestParam(defaultValue = "TND") String currency,
                                 @RequestParam(defaultValue = "false") boolean roundTrip) {
        return pricing.quote(airportId, zoneId, vehicleTypeId, currency, roundTrip ? 2 : 1);
    }
}
