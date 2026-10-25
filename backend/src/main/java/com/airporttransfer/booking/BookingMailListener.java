package com.airporttransfer.booking;

import com.airporttransfer.booking.BookingDtos.BookingDto;
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
 * Sends the confirmation e-mail once the booking is committed (MailHog in dev). A mail failure is logged and never
 * affects the booking. Texts live in messages*.properties (fr default, en, ar).
 */
@Component
public class BookingMailListener {

    private static final Logger log = LoggerFactory.getLogger(BookingMailListener.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final MessageSource messages;
    private final String from;

    public BookingMailListener(ObjectProvider<JavaMailSender> mailSender, MessageSource messages,
                               @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.messages = messages;
        this.from = from;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingsCreated(BookingsCreatedEvent event) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(event.email());
            mail.setSubject(messages.getMessage("mail.booking.subject",
                new Object[] {event.bookings().get(0).reference()}, event.locale()));
            mail.setText(body(event));
            sender.send(mail);
        } catch (RuntimeException e) {
            log.warn("Booking confirmation e-mail could not be sent to {}: {}", event.email(), e.getMessage());
        }
    }

    String body(BookingsCreatedEvent event) {
        Locale locale = event.locale();
        StringBuilder text = new StringBuilder(
            messages.getMessage("mail.booking.hello", new Object[] {event.fullName()}, locale)).append("\n\n");
        for (BookingDto b : event.bookings()) {
            text.append(MailTexts.leg(messages, b, locale)).append("\n");
        }
        text.append("\n").append(messages.getMessage("mail.booking.footer", null, locale));
        return text.toString();
    }
}
