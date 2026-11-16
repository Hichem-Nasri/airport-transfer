package com.airporttransfer.notification;

/**
 * Sends a text message. The real provider (Twilio, Brevo, ...) is plugged in later by providing another bean that
 * replaces {@link LoggingSmsGateway}; nothing else changes. Implementations throw when the message could not be
 * handed to the provider, so the reminder is retried.
 */
public interface SmsGateway {

    /** @param phone the number as the customer typed it; normalising to E.164 is the provider adapter's job */
    void send(String phone, String text);
}
