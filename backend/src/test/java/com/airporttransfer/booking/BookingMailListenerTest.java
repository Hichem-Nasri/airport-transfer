package com.airporttransfer.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airporttransfer.booking.BookingDtos.AirportSummary;
import com.airporttransfer.booking.BookingDtos.BookingDto;
import com.airporttransfer.booking.BookingDtos.VehicleTypeSummary;
import com.airporttransfer.booking.BookingDtos.ZoneSummary;
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

class BookingMailListenerTest {

    private final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
    private final JavaMailSender sender = mock(JavaMailSender.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);

    BookingMailListenerTest() {
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        when(provider.getIfAvailable()).thenReturn(sender);
    }

    private BookingsCreatedEvent event(Locale locale) {
        BookingDto dto = new BookingDto(1L, "MH-ABCD2345", null, TripDirection.AIRPORT_TO_ADDRESS,
            BookingStatus.PENDING, new AirportSummary(1L, "TUN", "Tunis-Carthage", "Tunis-Carthage Airport", "مطار تونس قرطاج"),
            new ZoneSummary(1L, "Z01", "Grand Tunis", "Greater Tunis", "تونس الكبرى"), "12 Rue de Marseille",
            "TU714",
            // 10:30 UTC is 11:30 in Tunis (UTC+1, no daylight saving time).
            Instant.parse("2026-12-01T10:30:00Z"), 2, 1,
            new VehicleTypeSummary(1L, "SEDAN", "Berline", "Sedan", "سيارة", 3), null, new BigDecimal("50.000"),
            "TND", null, new BigDecimal("50.000"), true, PaymentMethod.CASH, true, Instant.now());
        return new BookingsCreatedEvent("client@example.tn", "Ali Ben Salah", locale, List.of(dto));
    }

    private BookingMailListener listener() {
        return new BookingMailListener(provider, messages, "noreply@marhba.tn");
    }

    @Test
    void frenchIsTheDefaultLanguageAndPickupIsShownInTunisTime() {
        String body = listener().body(event(Locale.FRENCH));

        assertThat(body).contains("Bonjour Ali Ben Salah", "MH-ABCD2345", "Tunis-Carthage", "12 Rue de Marseille",
            "01/12/2026 11:30", "50.000 TND", "De l'aéroport vers l'adresse");
    }

    @Test
    void englishAndArabicTextsExist() {
        assertThat(listener().body(event(Locale.ENGLISH))).contains("Hello Ali Ben Salah", "Tunis-Carthage Airport",
            "From the airport to the address");
        assertThat(listener().body(event(Locale.forLanguageTag("ar")))).contains("مرحبا", "مطار تونس قرطاج",
            "من المطار إلى العنوان");
    }

    @Test
    void unknownLanguageFallsBackToFrench() {
        assertThat(listener().body(event(Locale.GERMAN))).contains("Bonjour");
    }

    @Test
    void mailIsSentToTheCustomerWithTheReferenceInTheSubject() {
        listener().onBookingsCreated(event(Locale.FRENCH));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("client@example.tn");
        assertThat(captor.getValue().getSubject()).contains("MH-ABCD2345");
        assertThat(captor.getValue().getFrom()).isEqualTo("noreply@marhba.tn");
    }

    @Test
    void aMailFailureNeverPropagates() {
        doThrow(new org.springframework.mail.MailSendException("smtp down")).when(sender)
            .send(any(SimpleMailMessage.class));

        assertThatCode(() -> listener().onBookingsCreated(event(Locale.FRENCH))).doesNotThrowAnyException();
    }
}
