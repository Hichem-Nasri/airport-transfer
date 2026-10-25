package com.airporttransfer.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** One successful payment of a booking, in the currency and amount of the booking's price snapshot. */
@Entity
@Table(name = "payments")
public class Payment {

    public enum Status { SUCCEEDED, REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false, updatable = false)
    private Long bookingId;

    @Column(nullable = false, updatable = false)
    private String provider;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.SUCCEEDED;

    @Column(name = "external_ref")
    private String externalRef;

    @Column(name = "recorded_by", updatable = false)
    private Long recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "refunded_at")
    private Instant refundedAt;

    protected Payment() {
    }

    public Payment(Long bookingId, String provider, BigDecimal amount, String currency, String externalRef,
                   Long recordedBy) {
        this.bookingId = bookingId;
        this.provider = provider;
        this.amount = amount;
        this.currency = currency;
        this.externalRef = externalRef;
        this.recordedBy = recordedBy;
    }

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getProvider() {
        return provider;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Status getStatus() {
        return status;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void markRefunded(String refundRef) {
        this.status = Status.REFUNDED;
        this.refundedAt = Instant.now();
        if (refundRef != null) {
            this.externalRef = refundRef;
        }
    }
}
