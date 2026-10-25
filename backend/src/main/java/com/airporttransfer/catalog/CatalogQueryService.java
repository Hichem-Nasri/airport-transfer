package com.airporttransfer.catalog;

import com.airporttransfer.catalog.CatalogDtos.AirportDto;
import com.airporttransfer.catalog.CatalogDtos.AreaDto;
import com.airporttransfer.catalog.CatalogDtos.VehicleTypeDto;
import com.airporttransfer.catalog.CatalogDtos.ZoneWithAreasDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only catalog for the public booking form: active items only. */
@Service
public class CatalogQueryService {

    private final AirportRepository airports;
    private final ZoneRepository zones;
    private final AreaRepository areas;
    private final VehicleTypeRepository vehicleTypes;
    private final CatalogMapper mapper;

    public CatalogQueryService(AirportRepository airports, ZoneRepository zones, AreaRepository areas,
                               VehicleTypeRepository vehicleTypes, CatalogMapper mapper) {
        this.airports = airports;
        this.zones = zones;
        this.areas = areas;
        this.vehicleTypes = vehicleTypes;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AirportDto> airports() {
        return airports.findAllByActiveTrueOrderByIataCodeAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleTypeDto> vehicleTypes() {
        return vehicleTypes.findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ZoneWithAreasDto> zones() {
        Map<Long, List<AreaDto>> areasByZone = new LinkedHashMap<>();
        for (Area area : areas.findAllByActiveTrueOrderByNameFrAscIdAsc()) {
            AreaDto dto = mapper.toDto(area);
            areasByZone.computeIfAbsent(dto.zoneId(), id -> new ArrayList<>()).add(dto);
        }
        return zones.findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
            .map(z -> new ZoneWithAreasDto(z.getId(), z.getCode(), z.getNameFr(), z.getNameEn(), z.getNameAr(),
                areasByZone.getOrDefault(z.getId(), List.of())))
            .toList();
    }
}
