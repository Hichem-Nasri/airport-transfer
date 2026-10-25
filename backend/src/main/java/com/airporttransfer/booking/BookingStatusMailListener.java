package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingEvents.DriverAssigned;
import com.airporttransfer.booking.BookingEvents.StatusChanged;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.UserDtos.PersonDto;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * E-mails sent after a status change commits: the customer (in the language of the booking) when it is
 * confirmed, cancelled by staff or completed, and when a driver is assigned; the driver (in French) when a trip is
 * assigned or cancelled. A mail failure is logged and never affects the booking.
 */
@Component
public class BookingStatusMailListener {

    private static final Logger log = LoggerFactory.getLogger(BookingStatusMailListener.class);
    static final Locale DRIVER_LOCALE = Locale.FRENCH;

    private final ObjectProvider<JavaMailSender> mailSender;
    private final MessageSource messages;
    private final String from;

    public BookingStatusMailListener(ObjectProvider<JavaMailSender> mailSender, MessageSource messages,
                                     @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.messages = messages;
        this.from = from;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(StatusChanged e) {
        String ref = e.booking().reference();
        switch (e.booking().status()) {
            case CONFIRMED -> toCustomer(e.customer(), e.customerLocale(), "confirmed", ref,
                e.customer().fullName(), MailTexts.leg(messages, e.booking(), e.customerLocale()));
            case COMPLETED -> toCustomer(e.customer(), e.customerLocale(), "completed", ref,
                e.customer().fullName(), MailTexts.leg(messages, e.booking(), e.customerLocale()));
            case CANCELLED -> cancelled(e);
            default -> { }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDriverAssigned(DriverAssigned e) {
        String ref = e.booking().reference();
        Locale customerLocale = e.customerLocale();
        toCustomer(e.customer(), customerLocale, "assigned", ref, e.customer().fullName(),
            MailTexts.leg(messages, e.booking(), customerLocale), e.driver().fullName(), phone(e.driver()));
        send(e.driver().email(), subject("mail.driver.assigned.subject", ref, DRIVER_LOCALE),
            messages.getMessage("mail.driver.assigned.body", new Object[] {
                e.driver().fullName(),
                MailTexts.leg(messages, e.booking(), DRIVER_LOCALE),
                e.customer().fullName(),
                phone(e.customer()),
                e.booking().passengers(),
                e.luggageCount()
            }, DRIVER_LOCALE));
    }

    private void cancelled(StatusChanged e) {
        String ref = e.booking().reference();
        // A customer who cancels themselves does not need an e-mail about it; staff cancellations do.
        if (e.actorRole() != Role.CUSTOMER) {
            Locale locale = e.customerLocale();
            String reason = e.note() == null ? ""
                : "\n" + messages.getMessage("mail.status.reason", new Object[] {e.note()}, locale);
            toCustomer(e.customer(), locale, "cancelled", ref, e.customer().fullName(),
                MailTexts.leg(messages, e.booking(), locale), reason);
        }
        if (e.driver() != null) {
            send(e.driver().email(), subject("mail.driver.cancelled.subject", ref, DRIVER_LOCALE),
                messages.getMessage("mail.driver.cancelled.body", new Object[] {
                    e.driver().fullName(), MailTexts.leg(messages, e.booking(), DRIVER_LOCALE)}, DRIVER_LOCALE));
        }
    }

    private void toCustomer(PersonDto customer, Locale locale, String kind, String reference, Object... bodyArgs) {
        if (customer == null) {
            return;
        }
        send(customer.email(), subject("mail.status." + kind + ".subject", reference, locale),
            messages.getMessage("mail.status." + kind + ".body", bodyArgs, locale));
    }

    private String subject(String key, String reference, Locale locale) {
        return messages.getMessage(key, new Object[] {reference}, locale);
    }

    private static String phone(PersonDto person) {
        return person.phone() == null ? "-" : person.phone();
    }

    private void send(String to, String subject, String text) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || to == null) {
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(text);
            sender.send(mail);
        } catch (RuntimeException ex) {
            log.warn("Booking e-mail could not be sent to {}: {}", to, ex.getMessage());
        }
    }
}
