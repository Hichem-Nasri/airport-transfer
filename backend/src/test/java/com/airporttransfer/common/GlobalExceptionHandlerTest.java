package com.airporttransfer.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource());

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private String messageIn(Locale locale, String code) {
        LocaleContextHolder.setLocale(locale);
        return handler.api(new ApiException(HttpStatus.NOT_FOUND, code, "English fallback")).getBody().message();
    }

    @Test
    void errorMessagesFollowTheCallersLanguageWhileTheCodeStaysStable() {
        assertThat(messageIn(Locale.FRENCH, "BOOKING_NOT_FOUND")).containsIgnoringCase("réservation");
        assertThat(messageIn(Locale.ENGLISH, "BOOKING_NOT_FOUND")).containsIgnoringCase("booking");
        assertThat(messageIn(Locale.forLanguageTag("ar"), "BOOKING_NOT_FOUND")).contains("الحجز");
        LocaleContextHolder.setLocale(Locale.FRENCH);
        assertThat(handler.api(new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "x")).getBody().code())
            .isEqualTo("BOOKING_NOT_FOUND");
    }

    @Test
    void unknownCodesFallBackToTheExceptionMessage() {
        assertThat(messageIn(Locale.FRENCH, "SOME_NEW_CODE")).isEqualTo("English fallback");
    }

    @Test
    void everyFrontendErrorCodeHasAMessageInTheThreeLanguages() {
        ResourceBundleMessageSource source = messageSource();
        for (String code : new String[] {"VALIDATION_FAILED", "BAD_REQUEST", "CONFLICT", "CONCURRENT_UPDATE",
            "UNAUTHORIZED", "FORBIDDEN", "TOO_MANY_REQUESTS", "INVALID_CREDENTIALS"}) {
            for (String lang : new String[] {"fr", "en", "ar"}) {
                assertThat(source.getMessage("error." + code, null, "MISSING", Locale.forLanguageTag(lang)))
                    .as("%s in %s", code, lang).isNotEqualTo("MISSING");
            }
        }
    }
}
