package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.AirportSummary;
import com.airporttransfer.booking.BookingDtos.BookingDto;
import com.airporttransfer.booking.BookingDtos.VehicleTypeSummary;
import com.airporttransfer.booking.BookingDtos.ZoneSummary;
import com.airporttransfer.catalog.Airport;
import com.airporttransfer.catalog.VehicleType;
import com.airporttransfer.catalog.Zone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class BookingMapper {

    @Autowired
    protected BookingStatusService statuses;

    @Mapping(target = "priceDemo", source = "priceDemo")
    @Mapping(target = "cancellable",
        expression = "java(statuses.canTransition(booking.getStatus(), BookingStatus.CANCELLED))")
    public abstract BookingDto toDto(Booking booking);

    abstract AirportSummary toSummary(Airport airport);

    abstract ZoneSummary toSummary(Zone zone);

    abstract VehicleTypeSummary toSummary(VehicleType vehicleType);
}
