package com.airporttransfer.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default gateway: sends nothing, only logs a masked number and the text. A real provider is added as another
 * {@link SmsGateway} bean enabled by its own {@code app.sms.provider} value (for example {@code twilio}).
 */
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "log", matchIfMissing = true)
class LoggingSmsGateway implements SmsGateway {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingSmsGateway.class);

    @Override
    public void send(String phone, String text) {
        LOG.info("SMS (not sent, no provider configured) to {}: {}", mask(phone), text);
    }

    static String mask(String phone) {
        String p = phone == null ? "" : phone.trim();
        return p.length() <= 4 ? "****" : "*".repeat(p.length() - 4) + p.substring(p.length() - 4);
    }
}
