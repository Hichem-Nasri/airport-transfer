package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.BookingDto;
import com.airporttransfer.user.UserDtos.PersonDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class StaffDtos {

    private StaffDtos() {
    }

    public record HistoryEntryDto(BookingStatus fromStatus, BookingStatus toStatus, Long actorId, String actorName,
                                  String actorRole, String note, Instant createdAt) {
    }

    /** A booking as staff see it. {@code history} is filled on the detail view only (null in lists). */
    public record StaffBookingDto(BookingDto booking, PersonDto customer, PersonDto driver,
                                  List<HistoryEntryDto> history) {
    }

    /** What a driver sees of one of their trips: the booking (amount to collect in cash) and the customer. */
    public record DriverTripDto(BookingDto booking, PersonDto customer) {
    }

    public record AssignDriverRequest(@NotNull Long driverId, boolean force) {
    }

    public record CancelRequest(@Size(max = 500) String reason) {
    }

    /** List filters. {@code from} is inclusive and {@code to} exclusive, on the pickup instant. */
    public record BookingFilter(BookingStatus status, Instant from, Instant to, Long airportId, Long driverId,
                                Boolean unassigned, String q) {
    }

}
