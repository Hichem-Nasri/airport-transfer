package com.airporttransfer.booking;

/** Allowed transitions are enforced in {@link BookingStatusService}, not here. */
public enum BookingStatus {
    PENDING, CONFIRMED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
}
