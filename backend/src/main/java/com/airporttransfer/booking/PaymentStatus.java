package com.airporttransfer.booking;

/** Whether the customer has paid. Independent from the trip lifecycle ({@link BookingStatus}). */
public enum PaymentStatus {
    UNPAID, PAID, REFUNDED
}
