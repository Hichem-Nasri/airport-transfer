package com.airporttransfer.user;

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
import java.util.Locale;

/** Sends the reset link after the token is committed. A mail failure is logged, never surfaced. */
@Component
public class PasswordResetMailListener {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailListener.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final MessageSource messages;
    private final String from;

    public PasswordResetMailListener(ObjectProvider<JavaMailSender> mailSender, MessageSource messages,
                                     @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.messages = messages;
        this.from = from;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(PasswordResetRequested e) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            return;
        }
        Locale locale = e.locale();
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(e.email());
            mail.setSubject(messages.getMessage("mail.reset.subject", null, locale));
            mail.setText(messages.getMessage("mail.reset.body", new Object[] {e.fullName(), e.link()}, locale));
            sender.send(mail);
        } catch (RuntimeException ex) {
            log.warn("Password reset e-mail could not be sent: {}", ex.getMessage());
        }
    }
}
