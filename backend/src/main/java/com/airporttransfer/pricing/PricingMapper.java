package com.airporttransfer.pricing;

import com.airporttransfer.pricing.PricingDtos.CurrencyDto;
import com.airporttransfer.pricing.PricingDtos.CurrencyRequest;
import com.airporttransfer.pricing.PricingDtos.ExchangeRateDto;
import com.airporttransfer.pricing.PricingDtos.TariffDto;
import com.airporttransfer.pricing.PricingDtos.TariffRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PricingMapper {

    CurrencyDto toDto(CurrencyDefinition currency);

    @Mapping(target = "code", ignore = true)
    void update(CurrencyRequest request, @MappingTarget CurrencyDefinition currency);

    ExchangeRateDto toDto(ExchangeRate rate);

    @Mapping(source = "airport.id", target = "airportId")
    @Mapping(source = "zone.id", target = "zoneId")
    @Mapping(source = "vehicleType.id", target = "vehicleTypeId")
    TariffDto toDto(Tariff tariff);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "airport", ignore = true)
    @Mapping(target = "zone", ignore = true)
    @Mapping(target = "vehicleType", ignore = true)
    @Mapping(target = "demo", ignore = true)
    void update(TariffRequest request, @MappingTarget Tariff tariff);
}
