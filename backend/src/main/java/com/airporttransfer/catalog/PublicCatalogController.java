package com.airporttransfer.catalog;

import com.airporttransfer.catalog.CatalogDtos.AirportDto;
import com.airporttransfer.catalog.CatalogDtos.VehicleTypeDto;
import com.airporttransfer.catalog.CatalogDtos.ZoneWithAreasDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicCatalogController {

    private final CatalogQueryService service;

    public PublicCatalogController(CatalogQueryService service) {
        this.service = service;
    }

    @GetMapping("/airports")
    public List<AirportDto> airports() {
        return service.airports();
    }

    @GetMapping("/zones")
    public List<ZoneWithAreasDto> zones() {
        return service.zones();
    }

    @GetMapping("/vehicle-types")
    public List<VehicleTypeDto> vehicleTypes() {
        return service.vehicleTypes();
    }
}
