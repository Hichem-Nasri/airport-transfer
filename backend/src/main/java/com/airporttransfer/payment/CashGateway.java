package com.airporttransfer.payment;

import com.airporttransfer.booking.PaymentMethod;
import org.springframework.stereotype.Component;

/** Cash paid to the driver. Nothing is charged online, and a refund is a manual operation done by staff. */
@Component
public class CashGateway implements PaymentGateway {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CASH;
    }

    @Override
    public String provider() {
        return "CASH";
    }

    @Override
    public boolean supports(String currency) {
        return true;
    }

    @Override
    public String refund(Payment payment) {
        return "manual";
    }
}
