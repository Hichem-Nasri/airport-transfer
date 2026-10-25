package com.airporttransfer.catalog;

import com.airporttransfer.catalog.CatalogDtos.AirportDto;
import com.airporttransfer.catalog.CatalogDtos.AirportRequest;
import com.airporttransfer.catalog.CatalogDtos.AreaDto;
import com.airporttransfer.catalog.CatalogDtos.AreaRequest;
import com.airporttransfer.catalog.CatalogDtos.VehicleTypeDto;
import com.airporttransfer.catalog.CatalogDtos.VehicleTypeRequest;
import com.airporttransfer.catalog.CatalogDtos.ZoneDto;
import com.airporttransfer.catalog.CatalogDtos.ZoneRequest;
import com.airporttransfer.common.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin CRUD for airports, zones, areas and vehicle types.
 * Duplicate codes and deleting something still referenced surface as a 409 (see GlobalExceptionHandler):
 * write operations flush immediately so the database constraint fires inside the call.
 */
@Service
public class CatalogAdminService {

    private final AirportRepository airports;
    private final ZoneRepository zones;
    private final AreaRepository areas;
    private final VehicleTypeRepository vehicleTypes;
    private final CatalogMapper mapper;

    public CatalogAdminService(AirportRepository airports, ZoneRepository zones, AreaRepository areas,
                               VehicleTypeRepository vehicleTypes, CatalogMapper mapper) {
        this.airports = airports;
        this.zones = zones;
        this.areas = areas;
        this.vehicleTypes = vehicleTypes;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- airports

    @Transactional(readOnly = true)
    public List<AirportDto> listAirports() {
        return airports.findAllByOrderByIataCodeAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public AirportDto createAirport(AirportRequest request) {
        Airport airport = new Airport();
        mapper.update(request, airport);
        return mapper.toDto(airports.saveAndFlush(airport));
    }

    @Transactional
    public AirportDto updateAirport(Long id, AirportRequest request) {
        Airport airport = airports.findById(id)
            .orElseThrow(() -> ApiException.notFound("AIRPORT_NOT_FOUND", "Unknown airport: " + id));
        mapper.update(request, airport);
        return mapper.toDto(airports.saveAndFlush(airport));
    }

    @Transactional
    public void deleteAirport(Long id) {
        Airport airport = airports.findById(id)
            .orElseThrow(() -> ApiException.notFound("AIRPORT_NOT_FOUND", "Unknown airport: " + id));
        airports.delete(airport);
        airports.flush();
    }

    // ---------------------------------------------------------------- zones

    @Transactional(readOnly = true)
    public List<ZoneDto> listZones() {
        return zones.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public ZoneDto createZone(ZoneRequest request) {
        Zone zone = new Zone();
        mapper.update(request, zone);
        return mapper.toDto(zones.saveAndFlush(zone));
    }

    @Transactional
    public ZoneDto updateZone(Long id, ZoneRequest request) {
        Zone zone = zones.findById(id)
            .orElseThrow(() -> ApiException.notFound("ZONE_NOT_FOUND", "Unknown zone: " + id));
        mapper.update(request, zone);
        return mapper.toDto(zones.saveAndFlush(zone));
    }

    @Transactional
    public void deleteZone(Long id) {
        Zone zone = zones.findById(id)
            .orElseThrow(() -> ApiException.notFound("ZONE_NOT_FOUND", "Unknown zone: " + id));
        zones.delete(zone);
        zones.flush();
    }

    // ---------------------------------------------------------------- areas

    @Transactional(readOnly = true)
    public List<AreaDto> listAreas() {
        return areas.findAllByOrderByNameFrAscIdAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public AreaDto createArea(AreaRequest request) {
        Area area = new Area();
        mapper.update(request, area);
        area.setZone(zoneOf(request));
        return mapper.toDto(areas.saveAndFlush(area));
    }

    /** Updating the zone of an area is how an admin regroups governorates and cities into zones. */
    @Transactional
    public AreaDto updateArea(Long id, AreaRequest request) {
        Area area = areas.findById(id)
            .orElseThrow(() -> ApiException.notFound("AREA_NOT_FOUND", "Unknown area: " + id));
        mapper.update(request, area);
        area.setZone(zoneOf(request));
        return mapper.toDto(areas.saveAndFlush(area));
    }

    @Transactional
    public void deleteArea(Long id) {
        Area area = areas.findById(id)
            .orElseThrow(() -> ApiException.notFound("AREA_NOT_FOUND", "Unknown area: " + id));
        areas.delete(area);
        areas.flush();
    }

    private Zone zoneOf(AreaRequest request) {
        return zones.findById(request.zoneId())
            .orElseThrow(() -> ApiException.notFound("ZONE_NOT_FOUND", "Unknown zone: " + request.zoneId()));
    }

    // ---------------------------------------------------------------- vehicle types

    @Transactional(readOnly = true)
    public List<VehicleTypeDto> listVehicleTypes() {
        return vehicleTypes.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public VehicleTypeDto createVehicleType(VehicleTypeRequest request) {
        checkCapacity(request);
        VehicleType type = new VehicleType();
        mapper.update(request, type);
        return mapper.toDto(vehicleTypes.saveAndFlush(type));
    }

    @Transactional
    public VehicleTypeDto updateVehicleType(Long id, VehicleTypeRequest request) {
        checkCapacity(request);
        VehicleType type = vehicleTypes.findById(id)
            .orElseThrow(() -> ApiException.notFound("VEHICLE_TYPE_NOT_FOUND", "Unknown vehicle type: " + id));
        mapper.update(request, type);
        return mapper.toDto(vehicleTypes.saveAndFlush(type));
    }

    @Transactional
    public void deleteVehicleType(Long id) {
        VehicleType type = vehicleTypes.findById(id)
            .orElseThrow(() -> ApiException.notFound("VEHICLE_TYPE_NOT_FOUND", "Unknown vehicle type: " + id));
        vehicleTypes.delete(type);
        vehicleTypes.flush();
    }

    private void checkCapacity(VehicleTypeRequest request) {
        if (request.maxPassengers() < request.minPassengers()) {
            throw ApiException.badRequest("INVALID_CAPACITY", "maxPassengers must be at least minPassengers");
        }
    }
}
