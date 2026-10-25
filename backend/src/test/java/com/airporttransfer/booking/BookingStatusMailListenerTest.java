package com.airporttransfer.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airporttransfer.booking.BookingDtos.AirportSummary;
import com.airporttransfer.booking.BookingDtos.BookingDto;
import com.airporttransfer.booking.BookingDtos.VehicleTypeSummary;
import com.airporttransfer.booking.BookingDtos.ZoneSummary;
import com.airporttransfer.booking.BookingEvents.DriverAssigned;
import com.airporttransfer.booking.BookingEvents.StatusChanged;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.UserDtos.PersonDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class BookingStatusMailListenerTest {

    private final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
    private final JavaMailSender sender = mock(JavaMailSender.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    private final PersonDto customer = new PersonDto(1L, "Ali Ben Salah", "client@example.tn", "+216 20 000 000");
    private final PersonDto driver = new PersonDto(2L, "Karim Driver", "driver@example.tn", "+216 98 111 222");

    BookingStatusMailListenerTest() {
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        when(provider.getIfAvailable()).thenReturn(sender);
    }

    private BookingDto booking(BookingStatus status) {
        return new BookingDto(1L, "MH-ABCD2345", null, TripDirection.AIRPORT_TO_ADDRESS, status,
            new AirportSummary(1L, "TUN", "Tunis-Carthage", "Tunis-Carthage Airport", "مطار تونس قرطاج"),
            new ZoneSummary(1L, "Z01", "Grand Tunis", "Greater Tunis", "تونس الكبرى"), "12 Rue de Marseille", null,
            Instant.parse("2026-12-01T10:30:00Z"), 2, 1,
            new VehicleTypeSummary(1L, "SEDAN", "Berline", "Sedan", "سيارة", 3), null, new BigDecimal("50.000"),
            "TND", null, new BigDecimal("50.000"), false, PaymentMethod.CASH, PaymentStatus.UNPAID, false, false, Instant.now());
    }

    private BookingStatusMailListener listener() {
        return new BookingStatusMailListener(provider, messages, "noreply@marhba.tn");
    }

    private List<SimpleMailMessage> sent(int expected) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, times(expected)).send(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void confirmationGoesToTheCustomerInTheirLanguage() {
        listener().onStatusChanged(new StatusChanged(booking(BookingStatus.CONFIRMED), customer, Locale.ENGLISH,
            null, null, Role.MODERATOR));

        SimpleMailMessage mail = sent(1).get(0);
        assertThat(mail.getTo()).containsExactly("client@example.tn");
        assertThat(mail.getSubject()).isEqualTo("Marhba - Booking MH-ABCD2345 confirmed");
        assertThat(mail.getText()).contains("Hello Ali Ben Salah", "01/12/2026 11:30", "Tunis-Carthage Airport");
    }

    @Test
    void assignmentTellsTheCustomerWhoComesAndTheDriverWhereToGo() {
        listener().onDriverAssigned(new DriverAssigned(booking(BookingStatus.ASSIGNED), customer, Locale.FRENCH,
            driver, 3));

        List<SimpleMailMessage> mails = sent(2);
        SimpleMailMessage toCustomer = mails.get(0);
        assertThat(toCustomer.getTo()).containsExactly("client@example.tn");
        assertThat(toCustomer.getText()).contains("Karim Driver", "+216 98 111 222", "MH-ABCD2345");
        SimpleMailMessage toDriver = mails.get(1);
        assertThat(toDriver.getTo()).containsExactly("driver@example.tn");
        assertThat(toDriver.getSubject()).contains("Nouvelle course");
        assertThat(toDriver.getText()).contains("Ali Ben Salah", "+216 20 000 000", "12 Rue de Marseille", "bagages : 3");
    }

    @Test
    void staffCancellationMailsTheCustomerWithTheReasonAndTheDriver() {
        listener().onStatusChanged(new StatusChanged(booking(BookingStatus.CANCELLED), customer, Locale.FRENCH,
            driver, "Pas de chauffeur disponible", Role.MODERATOR));

        List<SimpleMailMessage> mails = sent(2);
        assertThat(mails.get(0).getTo()).containsExactly("client@example.tn");
        assertThat(mails.get(0).getText()).contains("Motif : Pas de chauffeur disponible");
        assertThat(mails.get(1).getTo()).containsExactly("driver@example.tn");
    }

    @Test
    void customerWhoCancelsThemselvesGetsNoMailButTheDriverIsTold() {
        listener().onStatusChanged(new StatusChanged(booking(BookingStatus.CANCELLED), customer, Locale.FRENCH,
            driver, null, Role.CUSTOMER));

        List<SimpleMailMessage> mails = sent(1);
        assertThat(mails.get(0).getTo()).containsExactly("driver@example.tn");
    }

    @Test
    void cancellationWithoutDriverMailsOnlyTheCustomerAndWithoutReasonLine() {
        listener().onStatusChanged(new StatusChanged(booking(BookingStatus.CANCELLED), customer,
            Locale.forLanguageTag("ar"), null, null, Role.ADMIN));

        SimpleMailMessage mail = sent(1).get(0);
        assertThat(mail.getTo()).containsExactly("client@example.tn");
        assertThat(mail.getText()).doesNotContain("السبب");
    }

    @Test
    void completionThanksTheCustomer() {
        listener().onStatusChanged(new StatusChanged(booking(BookingStatus.COMPLETED), customer, Locale.FRENCH,
            driver, null, Role.DRIVER));

        assertThat(sent(1).get(0).getSubject()).contains("Merci pour votre trajet");
    }

    @Test
    void otherStatusesSendNothingAndFailuresNeverPropagate() {
        listener().onStatusChanged(new StatusChanged(booking(BookingStatus.IN_PROGRESS), customer, Locale.FRENCH,
            driver, null, Role.DRIVER));
        verify(sender, never()).send(any(SimpleMailMessage.class));

        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("down")).when(sender)
            .send(any(SimpleMailMessage.class));
        listener().onStatusChanged(new StatusChanged(booking(BookingStatus.CONFIRMED), customer, Locale.FRENCH,
            null, null, Role.MODERATOR));
    }
}
