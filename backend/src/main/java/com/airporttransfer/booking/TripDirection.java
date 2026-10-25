package com.airporttransfer.booking;

public enum TripDirection {
    AIRPORT_TO_ADDRESS, ADDRESS_TO_AIRPORT;

    public TripDirection opposite() {
        return this == AIRPORT_TO_ADDRESS ? ADDRESS_TO_AIRPORT : AIRPORT_TO_ADDRESS;
    }
}
