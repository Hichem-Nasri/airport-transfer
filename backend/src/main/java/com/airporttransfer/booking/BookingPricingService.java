package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.BookingQuoteDto;
import com.airporttransfer.booking.BookingDtos.PackQuoteDto;
import com.airporttransfer.booking.BookingDtos.PackQuoteLegRequest;
import com.airporttransfer.common.ApiException;
import com.airporttransfer.pricing.PricingDtos.QuoteDto;
import com.airporttransfer.pricing.QuoteService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Prices a one-way or round-trip booking. The preview shown to the customer and the amounts stored in the
 * bookings come from this same method, so they cannot differ. Each leg costs the same rounded amount, so the
 * total is exactly {@code legs × leg.amount}.
 */
@Service
public class BookingPricingService {

    private final QuoteService quotes;

    public BookingPricingService(QuoteService quotes) {
        this.quotes = quotes;
    }

    /** Prices every leg of a pack in one currency; the total is the sum of the (already rounded) leg amounts. */
    public PackQuoteDto quotePack(List<PackQuoteLegRequest> legs, String currency) {
        List<QuoteDto> quoted = legs.stream()
            .map(l -> quotes.quote(l.airportId(), l.zoneId(), l.vehicleTypeId(), currency))
            .toList();
        BigDecimal total = quoted.stream().map(QuoteDto::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        String code = quoted.isEmpty() ? currency : quoted.get(0).currency();
        return new PackQuoteDto(code, quoted, total, quoted.stream().anyMatch(QuoteDto::demo));
    }

    public BookingQuoteDto quote(Long airportId, Long zoneId, Long vehicleTypeId, String currency, int legs) {
        if (legs != 1 && legs != 2) {
            throw ApiException.badRequest("BAD_REQUEST", "A booking has one or two legs");
        }
        QuoteDto leg = quotes.quote(airportId, zoneId, vehicleTypeId, currency);
        return new BookingQuoteDto(leg, legs, leg.amount().multiply(BigDecimal.valueOf(legs)));
    }
}
