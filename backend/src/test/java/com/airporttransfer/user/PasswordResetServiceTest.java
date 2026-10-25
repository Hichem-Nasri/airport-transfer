package com.airporttransfer.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airporttransfer.common.ApiException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class PasswordResetServiceTest {

    UserRepository users = mock(UserRepository.class);
    PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    UserService userService = mock(UserService.class);
    ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    PasswordResetService service =
        new PasswordResetService(users, tokens, userService, events, "https://marhba.example/");

    private User user(boolean enabled) {
        User u = new User("a@example.tn", "hash", "Ali", null, Role.CUSTOMER);
        ReflectionTestUtils.setField(u, "id", 5L);
        ReflectionTestUtils.setField(u, "enabled", enabled);
        when(users.findByEmailIgnoreCase("a@example.tn")).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void unknownOrDisabledAccountsGetNothingAndNoError() {
        when(users.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        service.request("nobody@example.tn", Locale.FRENCH);
        user(false);
        service.request("A@Example.tn ", Locale.FRENCH);
        verify(tokens, never()).save(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void storesOnlyTheHashAndMailsTheLinkWithTheRawToken() {
        user(true);
        when(tokens.findByUserIdAndUsedAtIsNullAndExpiresAtAfter(anyLong(), any())).thenReturn(List.of());

        service.request("a@example.tn", Locale.ENGLISH);

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(saved.capture());
        ArgumentCaptor<PasswordResetRequested> event = ArgumentCaptor.forClass(PasswordResetRequested.class);
        verify(events).publishEvent(event.capture());
        String link = event.getValue().link();
        assertThat(link).startsWith("https://marhba.example/reset-password?token=");
        String raw = link.substring(link.indexOf("token=") + 6);
        assertThat(raw).hasSizeGreaterThanOrEqualTo(43);
        assertThat(ReflectionTestUtils.getField(saved.getValue(), "tokenHash"))
            .isEqualTo(PasswordResetService.hash(raw)).isNotEqualTo(raw);
        assertThat(event.getValue().locale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void askingAgainInvalidatesThePreviousLinks() {
        user(true);
        PasswordResetToken old = new PasswordResetToken(5L, "x", Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokens.findByUserIdAndUsedAtIsNullAndExpiresAtAfter(anyLong(), any())).thenReturn(List.of(old));
        service.request("a@example.tn", Locale.FRENCH);
        assertThat(old.isUsable(Instant.now())).isFalse();
    }

    @Test
    void resetSetsThePasswordOnceWithAValidToken() {
        PasswordResetToken t = new PasswordResetToken(5L, PasswordResetService.hash("tok"),
            Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokens.findByTokenHash(PasswordResetService.hash("tok"))).thenReturn(Optional.of(t));

        service.reset("tok", "new-password-1");

        verify(userService).setPassword(5L, "new-password-1");
        assertThatThrownBy(() -> service.reset("tok", "another-pass-2"))
            .isInstanceOf(ApiException.class).extracting("code").isEqualTo("RESET_TOKEN_INVALID");
        verify(userService, never()).setPassword(eq(5L), eq("another-pass-2"));
    }

    @Test
    void unknownAndExpiredTokensAreRejectedTheSameWay() {
        when(tokens.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reset("nope", "new-password-1"))
            .isInstanceOf(ApiException.class).extracting("code").isEqualTo("RESET_TOKEN_INVALID");

        PasswordResetToken expired = new PasswordResetToken(5L, PasswordResetService.hash("old"),
            Instant.now().minus(1, ChronoUnit.MINUTES));
        when(tokens.findByTokenHash(PasswordResetService.hash("old"))).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.reset("old", "new-password-1"))
            .isInstanceOf(ApiException.class).extracting("code").isEqualTo("RESET_TOKEN_INVALID");
        verify(userService, never()).setPassword(anyLong(), any());
    }
}
