package com.airporttransfer.booking;

import com.airporttransfer.catalog.Airport;
import com.airporttransfer.catalog.VehicleType;
import com.airporttransfer.catalog.Zone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String reference;

    /** Shared by the two legs of a round trip; null for a one-way booking. */
    @Column(name = "group_id", updatable = false)
    private UUID groupId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "airport_id")
    private Airport airport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripDirection direction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(nullable = false)
    private String address;

    @Column(name = "flight_number")
    private String flightNumber;

    /** Stored as UTC; the UI shows it in Africa/Tunis. */
    @Column(name = "pickup_at", nullable = false)
    private Instant pickupAt;

    @Column(nullable = false)
    private int passengers;

    @Column(name = "luggage_count", nullable = false)
    private int luggageCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    private String notes;

    // ---- price snapshot (never recomputed)
    @Column(name = "price_tnd", nullable = false, updatable = false)
    private BigDecimal priceTnd;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    /** TND per 1 unit of {@link #currency}; null when the currency is TND. */
    @Column(name = "exchange_rate", updatable = false)
    private BigDecimal exchangeRate;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "price_is_demo", nullable = false, updatable = false)
    private boolean priceDemo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "assigned_driver_id")
    private Long assignedDriverId;

    /** Language of the customer's confirmation e-mail and later notifications: fr, en or ar. */
    @Column(name = "customer_language", nullable = false)
    private String customerLanguage = "fr";

    /** Optimistic lock: two staff members acting on the same booking at once cannot both win. */
    @jakarta.persistence.Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Booking() {
    }

    /** Builder-style constructor used by {@link BookingService}; the price snapshot is fixed at creation. */
    public Booking(String reference, UUID groupId, Long customerId, Airport airport, TripDirection direction,
                   Zone zone, String address, String flightNumber, Instant pickupAt, int passengers,
                   int luggageCount, VehicleType vehicleType, String notes, BigDecimal priceTnd, String currency,
                   BigDecimal exchangeRate, BigDecimal amount, boolean priceDemo, PaymentMethod paymentMethod) {
        this.reference = reference;
        this.groupId = groupId;
        this.customerId = customerId;
        this.airport = airport;
        this.direction = direction;
        this.zone = zone;
        this.address = address;
        this.flightNumber = flightNumber;
        this.pickupAt = pickupAt;
        this.passengers = passengers;
        this.luggageCount = luggageCount;
        this.vehicleType = vehicleType;
        this.notes = notes;
        this.priceTnd = priceTnd;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.amount = amount;
        this.priceDemo = priceDemo;
        this.paymentMethod = paymentMethod;
    }

    /** Only {@link BookingStatusService} may call this: it is the single place that enforces transitions. */
    void changeStatus(BookingStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    /** Only PaymentService calls these: payment is independent from the trip lifecycle. */
    public void markPaid(Instant at) {
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAt = at;
        this.updatedAt = Instant.now();
    }

    public void markRefunded() {
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    void assignDriver(Long driverId) {
        this.assignedDriverId = driverId;
        this.updatedAt = Instant.now();
    }

    /** Practical details only; the price snapshot, vehicle, zone and passengers never change here. */
    void applyCustomerEdit(String address, String flightNumber, Instant pickupAt, int luggageCount, String notes) {
        this.address = address;
        this.flightNumber = flightNumber;
        this.pickupAt = pickupAt;
        this.luggageCount = luggageCount;
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    void setCustomerLanguage(String language) {
        this.customerLanguage = language;
    }

    public String getCustomerLanguage() {
        return customerLanguage;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Airport getAirport() {
        return airport;
    }

    public TripDirection getDirection() {
        return direction;
    }

    public Zone getZone() {
        return zone;
    }

    public String getAddress() {
        return address;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public Instant getPickupAt() {
        return pickupAt;
    }

    public int getPassengers() {
        return passengers;
    }

    public int getLuggageCount() {
        return luggageCount;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getNotes() {
        return notes;
    }

    public BigDecimal getPriceTnd() {
        return priceTnd;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isPriceDemo() {
        return priceDemo;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Long getAssignedDriverId() {
        return assignedDriverId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
