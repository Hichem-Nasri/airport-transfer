package com.airporttransfer.catalog;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    // ---- airports
    public record AirportDto(Long id, String iataCode, String nameFr, String nameEn, String nameAr,
                             BigDecimal latitude, BigDecimal longitude, boolean active) {
    }

    public record AirportRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String iataCode,
        @NotBlank @Size(max = 150) String nameFr,
        @NotBlank @Size(max = 150) String nameEn,
        @NotBlank @Size(max = 150) String nameAr,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @NotNull Boolean active) {
    }

    // ---- zones and areas
    public record ZoneDto(Long id, String code, String nameFr, String nameEn, String nameAr,
                          int displayOrder, boolean active) {
    }

    public record ZoneRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,20}") String code,
        @NotBlank @Size(max = 100) String nameFr,
        @NotBlank @Size(max = 100) String nameEn,
        @NotBlank @Size(max = 100) String nameAr,
        @NotNull @Min(0) Integer displayOrder,
        @NotNull Boolean active) {
    }

    public record AreaDto(Long id, String code, String nameFr, String nameEn, String nameAr,
                          Long zoneId, boolean active) {
    }

    public record AreaRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,30}") String code,
        @NotBlank @Size(max = 100) String nameFr,
        @NotBlank @Size(max = 100) String nameEn,
        @NotBlank @Size(max = 100) String nameAr,
        @NotNull Long zoneId,
        @NotNull Boolean active) {
    }

    /** Public view: a pricing zone with the active areas a customer can pick in it. */
    public record ZoneWithAreasDto(Long id, String code, String nameFr, String nameEn, String nameAr,
                                   List<AreaDto> areas) {
    }

    // ---- vehicle types
    public record VehicleTypeDto(Long id, String code, String nameFr, String nameEn, String nameAr,
                                 int minPassengers, int maxPassengers, int displayOrder, boolean active) {
    }

    public record VehicleTypeRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,20}") String code,
        @NotBlank @Size(max = 100) String nameFr,
        @NotBlank @Size(max = 100) String nameEn,
        @NotBlank @Size(max = 100) String nameAr,
        @NotNull @Min(1) Integer minPassengers,
        @NotNull @Min(1) Integer maxPassengers,
        @NotNull @Min(0) Integer displayOrder,
        @NotNull Boolean active) {
    }
}
