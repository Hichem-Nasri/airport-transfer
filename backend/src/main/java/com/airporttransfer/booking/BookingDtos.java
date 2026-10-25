package com.airporttransfer.booking;

import com.airporttransfer.pricing.PricingDtos.QuoteDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class BookingDtos {

    private BookingDtos() {
    }

    /**
     * {@code direction} is the direction of the first leg. When {@code returnPickupAt} is set the request is a
     * round trip: a second booking is created in the opposite direction at that time, linked by a group id.
     */
    public record CreateBookingRequest(
        @NotNull Long airportId,
        @NotNull TripDirection direction,
        @NotNull Long zoneId,
        @NotBlank @Size(max = 500) String address,
        @Pattern(regexp = "^$|^[A-Za-z0-9 ]{2,10}$") String flightNumber,
        @NotNull Instant pickupAt,
        @NotNull @Min(1) @Max(100) Integer passengers,
        @Min(0) @Max(100) Integer luggageCount,
        @NotNull Long vehicleTypeId,
        @Size(max = 1000) String notes,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull PaymentMethod paymentMethod,
        Instant returnPickupAt,
        @Pattern(regexp = "^$|^[A-Za-z0-9 ]{2,10}$") String returnFlightNumber) {
    }

    public record AirportSummary(Long id, String iataCode, String nameFr, String nameEn, String nameAr) {
    }

    public record ZoneSummary(Long id, String code, String nameFr, String nameEn, String nameAr) {
    }

    public record VehicleTypeSummary(Long id, String code, String nameFr, String nameEn, String nameAr,
                                     int maxPassengers) {
    }

    /** A booking as shown to its customer. {@code amount} is what is charged, in {@code currency}. */
    public record BookingDto(
        Long id, String reference, UUID groupId, TripDirection direction, BookingStatus status,
        AirportSummary airport, ZoneSummary zone, String address, String flightNumber,
        Instant pickupAt, int passengers, int luggageCount, VehicleTypeSummary vehicleType, String notes,
        BigDecimal priceTnd, String currency, BigDecimal exchangeRate, BigDecimal amount, boolean priceDemo,
        PaymentMethod paymentMethod, boolean cancellable, boolean editable, Instant createdAt) {
    }

    /**
     * Customer edit of a booking that is still PENDING or CONFIRMED. The vehicle, destination, passengers and price
     * cannot change (to change them, cancel and rebook), so the price snapshot stays valid.
     */
    public record UpdateBookingRequest(
        @NotBlank @Size(max = 500) String address,
        @Pattern(regexp = "^$|^[A-Za-z0-9 ]{2,10}$") String flightNumber,
        @NotNull Instant pickupAt,
        @NotNull @Min(0) @Max(100) Integer luggageCount,
        @Size(max = 1000) String notes) {
    }

    public record BookingsCreatedDto(UUID groupId, List<BookingDto> bookings) {
    }

    /** Price of one leg, the number of legs and the total, all computed by the server. */
    public record BookingQuoteDto(QuoteDto leg, int legs, BigDecimal totalAmount) {
    }
}
