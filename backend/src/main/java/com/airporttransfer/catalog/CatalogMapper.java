package com.airporttransfer.catalog;

import com.airporttransfer.catalog.CatalogDtos.AirportDto;
import com.airporttransfer.catalog.CatalogDtos.AirportRequest;
import com.airporttransfer.catalog.CatalogDtos.AreaDto;
import com.airporttransfer.catalog.CatalogDtos.AreaRequest;
import com.airporttransfer.catalog.CatalogDtos.VehicleTypeDto;
import com.airporttransfer.catalog.CatalogDtos.VehicleTypeRequest;
import com.airporttransfer.catalog.CatalogDtos.ZoneDto;
import com.airporttransfer.catalog.CatalogDtos.ZoneRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

    AirportDto toDto(Airport airport);

    @Mapping(target = "id", ignore = true)
    void update(AirportRequest request, @MappingTarget Airport airport);

    ZoneDto toDto(Zone zone);

    @Mapping(target = "id", ignore = true)
    void update(ZoneRequest request, @MappingTarget Zone zone);

    @Mapping(source = "zone.id", target = "zoneId")
    AreaDto toDto(Area area);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "zone", ignore = true)
    void update(AreaRequest request, @MappingTarget Area area);

    VehicleTypeDto toDto(VehicleType vehicleType);

    @Mapping(target = "id", ignore = true)
    void update(VehicleTypeRequest request, @MappingTarget VehicleType vehicleType);
}
