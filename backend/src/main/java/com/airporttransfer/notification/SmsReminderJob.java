package com.airporttransfer.notification;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls for due reminders. Off unless {@code SMS_ENABLED=true}, so dev and tests never schedule anything. */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.sms.enabled", havingValue = "true")
class SmsReminderJob {

    private static final Logger LOG = LoggerFactory.getLogger(SmsReminderJob.class);

    private final SmsReminderService service;

    SmsReminderJob(SmsReminderService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.sms.poll-interval-ms:60000}", initialDelay = 30_000)
    void run() {
        try {
            int sent = service.sendDue(Instant.now());
            if (sent > 0) {
                LOG.info("Sent {} SMS reminder(s)", sent);
            }
        } catch (RuntimeException e) {
            LOG.error("SMS reminder run failed", e);
        }
    }
}
