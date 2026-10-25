package com.airporttransfer.user;

import com.airporttransfer.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Forgot-password flow. {@link #request} never reveals whether an e-mail has an account. Tokens are 256-bit random,
 * stored hashed, valid for one hour and usable once; asking again invalidates the previous links.
 */
@Service
public class PasswordResetService {

    static final Duration VALIDITY = Duration.ofHours(1);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final UserService userService;
    private final ApplicationEventPublisher events;
    private final String publicUrl;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, UserService userService,
                                ApplicationEventPublisher events, @Value("${app.public-url}") String publicUrl) {
        this.users = users;
        this.tokens = tokens;
        this.userService = userService;
        this.events = events;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    @Transactional
    public void request(String email, Locale locale) {
        User user = users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT)).filter(User::isEnabled)
            .orElse(null);
        if (user == null) {
            return;
        }
        Instant now = Instant.now();
        tokens.findByUserIdAndUsedAtIsNullAndExpiresAtAfter(user.getId(), now).forEach(t -> t.markUsed(now));
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(new PasswordResetToken(user.getId(), hash(token), now.plus(VALIDITY)));
        events.publishEvent(new PasswordResetRequested(user.getEmail(), user.getFullName(), locale,
            publicUrl + "/reset-password?token=" + token));
    }

    /** Sets the new password when the token is valid; every failure looks the same (400 RESET_TOKEN_INVALID). */
    @Transactional
    public void reset(String token, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken stored = tokens.findByTokenHash(hash(token)).filter(t -> t.isUsable(now))
            .orElseThrow(() -> ApiException.badRequest("RESET_TOKEN_INVALID", "This link is invalid or has expired"));
        userService.setPassword(stored.getUserId(), newPassword);
        stored.markUsed(now);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
