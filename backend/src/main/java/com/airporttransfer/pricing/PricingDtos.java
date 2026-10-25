package com.airporttransfer.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class PricingDtos {

    private PricingDtos() {
    }

    // ---- currencies
    public record CurrencyDto(String code, String name, String symbol, int decimals, boolean active) {
    }

    public record CurrencyRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String code,
        @NotBlank @Size(max = 60) String name,
        @NotBlank @Size(max = 8) String symbol,
        @NotNull @Min(0) @Max(3) Integer decimals,
        @NotNull Boolean active) {
    }

    // ---- exchange rates (TND per 1 unit of the currency)
    public record ExchangeRateDto(Long id, String currencyCode, BigDecimal tndPerUnit, boolean demo, Instant createdAt) {
    }

    public record ExchangeRateRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currencyCode,
        @NotNull @DecimalMin("0.000001") @Digits(integer = 8, fraction = 6) BigDecimal tndPerUnit) {
    }

    // ---- tariffs (always in TND)
    public record TariffDto(Long id, Long airportId, Long zoneId, Long vehicleTypeId, BigDecimal priceTnd,
                            boolean active, boolean demo) {
    }

    public record TariffRequest(
        @NotNull Long airportId,
        @NotNull Long zoneId,
        @NotNull Long vehicleTypeId,
        @NotNull @DecimalMin("0.001") @Digits(integer = 7, fraction = 3) BigDecimal priceTnd,
        @NotNull Boolean active) {
    }

    /**
     * A price quote. {@code priceTnd} is the base price, {@code amount} the price in {@code currency}
     * (rounded HALF_UP to the currency's decimals), {@code exchangeRate} the TND-per-unit rate used
     * (null when the currency is TND). {@code demo} is true when a placeholder tariff or rate was used.
     */
    public record QuoteDto(Long airportId, Long zoneId, Long vehicleTypeId, BigDecimal priceTnd,
                           String currency, BigDecimal amount, BigDecimal exchangeRate, boolean demo) {
    }
}
