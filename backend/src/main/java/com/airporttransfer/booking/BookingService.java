package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.BookingDto;
import com.airporttransfer.booking.BookingDtos.BookingQuoteDto;
import com.airporttransfer.booking.BookingDtos.BookingsCreatedDto;
import com.airporttransfer.booking.BookingDtos.CreateBookingRequest;
import com.airporttransfer.catalog.Airport;
import com.airporttransfer.catalog.AirportRepository;
import com.airporttransfer.catalog.VehicleType;
import com.airporttransfer.catalog.VehicleTypeRepository;
import com.airporttransfer.catalog.Zone;
import com.airporttransfer.catalog.ZoneRepository;
import com.airporttransfer.common.ApiException;
import com.airporttransfer.pricing.PricingDtos.QuoteDto;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Booking rules. Controllers only translate HTTP; ownership is checked here (a customer sees only their own). */
@Service
public class BookingService {

    /** Card payments arrive with step 5; until then a card booking could never be paid, so it is refused. */
    static final Set<PaymentMethod> ENABLED_PAYMENT_METHODS = Set.of(PaymentMethod.CASH);

    private final BookingRepository bookings;
    private final AirportRepository airports;
    private final ZoneRepository zones;
    private final VehicleTypeRepository vehicleTypes;
    private final BookingPricingService pricing;
    private final BookingWorkflowService workflow;
    private final ReferenceGenerator references;
    private final BookingMapper mapper;
    private final UserService users;
    private final ApplicationEventPublisher events;

    public BookingService(BookingRepository bookings, AirportRepository airports, ZoneRepository zones,
                          VehicleTypeRepository vehicleTypes, BookingPricingService pricing,
                          BookingWorkflowService workflow, ReferenceGenerator references, BookingMapper mapper,
                          UserService users, ApplicationEventPublisher events) {
        this.bookings = bookings;
        this.airports = airports;
        this.zones = zones;
        this.vehicleTypes = vehicleTypes;
        this.pricing = pricing;
        this.workflow = workflow;
        this.references = references;
        this.mapper = mapper;
        this.users = users;
        this.events = events;
    }

    @Transactional
    public BookingsCreatedDto create(Long customerId, CreateBookingRequest request) {
        Instant now = Instant.now();
        boolean roundTrip = request.returnPickupAt() != null;
        if (!request.pickupAt().isAfter(now)) {
            throw ApiException.badRequest("PICKUP_IN_PAST", "The pickup time must be in the future");
        }
        if (roundTrip && !request.returnPickupAt().isAfter(request.pickupAt())) {
            throw ApiException.badRequest("RETURN_BEFORE_OUTBOUND", "The return must be after the first trip");
        }
        if (!ENABLED_PAYMENT_METHODS.contains(request.paymentMethod())) {
            throw ApiException.badRequest("PAYMENT_METHOD_NOT_AVAILABLE",
                "This payment method is not available yet");
        }

        User customer = users.getActive(customerId);
        Airport airport = airports.findByIdAndActiveTrue(request.airportId())
            .orElseThrow(() -> ApiException.notFound("AIRPORT_NOT_FOUND", "Unknown or inactive airport"));
        Zone zone = zones.findByIdAndActiveTrue(request.zoneId())
            .orElseThrow(() -> ApiException.notFound("ZONE_NOT_FOUND", "Unknown or inactive zone"));
        VehicleType vehicle = vehicleTypes.findByIdAndActiveTrue(request.vehicleTypeId())
            .orElseThrow(() -> ApiException.notFound("VEHICLE_TYPE_NOT_FOUND", "Unknown or inactive vehicle type"));
        if (request.passengers() > vehicle.getMaxPassengers()) {
            throw ApiException.badRequest("VEHICLE_CAPACITY_EXCEEDED",
                "This vehicle carries at most " + vehicle.getMaxPassengers() + " passengers");
        }

        // One quote for both legs: same rate, same rounding, same snapshot.
        BookingQuoteDto quote = pricing.quote(airport.getId(), zone.getId(), vehicle.getId(),
            request.currency(), roundTrip ? 2 : 1);
        QuoteDto leg = quote.leg();

        UUID groupId = roundTrip ? UUID.randomUUID() : null;
        int luggage = request.luggageCount() == null ? 0 : request.luggageCount();
        String address = request.address().trim();
        String notes = blankToNull(request.notes());

        List<Booking> legs = roundTrip
            ? List.of(
                build(groupId, customerId, airport, request.direction(), zone, address,
                    request.flightNumber(), request.pickupAt(), request, luggage, vehicle, notes, leg),
                build(groupId, customerId, airport, request.direction().opposite(), zone, address,
                    request.returnFlightNumber(), request.returnPickupAt(), request, luggage, vehicle, notes, leg))
            : List.of(build(groupId, customerId, airport, request.direction(), zone, address,
                request.flightNumber(), request.pickupAt(), request, luggage, vehicle, notes, leg));

        String language = language(LocaleContextHolder.getLocale());
        legs.forEach(l -> l.setCustomerLanguage(language));
        List<Booking> saved = bookings.saveAllAndFlush(legs);
        Actor actor = new Actor(customerId, Role.CUSTOMER);
        saved.forEach(b -> workflow.recordCreated(b, actor));
        List<BookingDto> created = saved.stream().map(mapper::toDto).toList();
        events.publishEvent(new BookingsCreatedEvent(customer.getEmail(), customer.getFullName(),
            LocaleContextHolder.getLocale(), created));
        return new BookingsCreatedDto(groupId, created);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> listMine(Long customerId) {
        return bookings.findByCustomerIdOrderByPickupAtDescIdDesc(customerId).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BookingDto getMine(Long customerId, Long id) {
        return mapper.toDto(findMine(customerId, id));
    }

    /** Cancels one of the customer's own bookings; legal only before IN_PROGRESS (see BookingStatusService). */
    @Transactional
    public BookingDto cancel(Long customerId, Long id) {
        Booking booking = findMine(customerId, id);
        workflow.changeStatus(booking, BookingStatus.CANCELLED, new Actor(customerId, Role.CUSTOMER), null);
        return mapper.toDto(booking);
    }

    /** Someone else's booking looks exactly like a missing one, so ids cannot be probed. */
    private Booking findMine(Long customerId, Long id) {
        return bookings.findByIdAndCustomerId(id, customerId)
            .orElseThrow(() -> ApiException.notFound("BOOKING_NOT_FOUND", "Booking not found"));
    }

    private Booking build(UUID groupId, Long customerId, Airport airport, TripDirection direction, Zone zone,
                          String address, String flightNumber, Instant pickupAt, CreateBookingRequest request,
                          int luggage, VehicleType vehicle, String notes, QuoteDto leg) {
        String reference = references.nextFree(bookings::existsByReference);
        return new Booking(reference, groupId, customerId, airport, direction, zone, address,
            normalizeFlight(flightNumber), pickupAt, request.passengers(), luggage, vehicle, notes,
            leg.priceTnd(), leg.currency(), leg.exchangeRate(), leg.amount(), leg.demo(),
            request.paymentMethod());
    }

    /** Only fr, en and ar have mail texts; anything else (including a missing header) means French. */
    private static String language(Locale locale) {
        String lang = locale.getLanguage();
        return lang.equals("en") || lang.equals("ar") ? lang : "fr";
    }

    private static String normalizeFlight(String flight) {
        String value = blankToNull(flight);
        return value == null ? null : value.replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
