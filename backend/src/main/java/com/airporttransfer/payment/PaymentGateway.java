package com.airporttransfer.payment;

import com.airporttransfer.booking.PaymentMethod;

/**
 * A way of getting paid. Every provider (cash, Stripe for EUR and USD cards, a local TND gateway later) sits behind
 * this interface so the booking code never knows which one it talks to. A booking is marked paid only by the server
 * (driver or staff for cash, a verified webhook for cards), never because the browser says so.
 */
public interface PaymentGateway {

    /** The payment method this gateway serves. */
    PaymentMethod method();

    /** Stable provider name stored with each payment (for example CASH or STRIPE). */
    String provider();

    /** Whether the gateway can charge in this currency (ISO code). */
    boolean supports(String currency);

    /** Gives the money back for a succeeded payment and returns the provider's reference (may be null). */
    String refund(Payment payment);
}
