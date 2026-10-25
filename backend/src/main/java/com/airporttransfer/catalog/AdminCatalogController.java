package com.airporttransfer.catalog;

import com.airporttransfer.catalog.CatalogDtos.AirportDto;
import com.airporttransfer.catalog.CatalogDtos.AirportRequest;
import com.airporttransfer.catalog.CatalogDtos.AreaDto;
import com.airporttransfer.catalog.CatalogDtos.AreaRequest;
import com.airporttransfer.catalog.CatalogDtos.VehicleTypeDto;
import com.airporttransfer.catalog.CatalogDtos.VehicleTypeRequest;
import com.airporttransfer.catalog.CatalogDtos.ZoneDto;
import com.airporttransfer.catalog.CatalogDtos.ZoneRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin CRUD for the catalog. Access is restricted to ADMIN in SecurityConfig (/api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {

    private final CatalogAdminService service;

    public AdminCatalogController(CatalogAdminService service) {
        this.service = service;
    }

    // ---- airports
    @GetMapping("/airports")
    public List<AirportDto> airports() {
        return service.listAirports();
    }

    @PostMapping("/airports")
    @ResponseStatus(HttpStatus.CREATED)
    public AirportDto createAirport(@Valid @RequestBody AirportRequest request) {
        return service.createAirport(request);
    }

    @PutMapping("/airports/{id}")
    public AirportDto updateAirport(@PathVariable Long id, @Valid @RequestBody AirportRequest request) {
        return service.updateAirport(id, request);
    }

    @DeleteMapping("/airports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAirport(@PathVariable Long id) {
        service.deleteAirport(id);
    }

    // ---- zones
    @GetMapping("/zones")
    public List<ZoneDto> zones() {
        return service.listZones();
    }

    @PostMapping("/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneDto createZone(@Valid @RequestBody ZoneRequest request) {
        return service.createZone(request);
    }

    @PutMapping("/zones/{id}")
    public ZoneDto updateZone(@PathVariable Long id, @Valid @RequestBody ZoneRequest request) {
        return service.updateZone(id, request);
    }

    @DeleteMapping("/zones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteZone(@PathVariable Long id) {
        service.deleteZone(id);
    }

    // ---- areas
    @GetMapping("/areas")
    public List<AreaDto> areas() {
        return service.listAreas();
    }

    @PostMapping("/areas")
    @ResponseStatus(HttpStatus.CREATED)
    public AreaDto createArea(@Valid @RequestBody AreaRequest request) {
        return service.createArea(request);
    }

    @PutMapping("/areas/{id}")
    public AreaDto updateArea(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        return service.updateArea(id, request);
    }

    @DeleteMapping("/areas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArea(@PathVariable Long id) {
        service.deleteArea(id);
    }

    // ---- vehicle types
    @GetMapping("/vehicle-types")
    public List<VehicleTypeDto> vehicleTypes() {
        return service.listVehicleTypes();
    }

    @PostMapping("/vehicle-types")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleTypeDto createVehicleType(@Valid @RequestBody VehicleTypeRequest request) {
        return service.createVehicleType(request);
    }

    @PutMapping("/vehicle-types/{id}")
    public VehicleTypeDto updateVehicleType(@PathVariable Long id, @Valid @RequestBody VehicleTypeRequest request) {
        return service.updateVehicleType(id, request);
    }

    @DeleteMapping("/vehicle-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicleType(@PathVariable Long id) {
        service.deleteVehicleType(id);
    }
}
