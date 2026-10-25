package com.airporttransfer.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import javax.crypto.SecretKey;

/** Pure unit test: no Spring context, no database. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-with-at-least-32-characters";

    private final JwtConfig config = new JwtConfig();

    private JwtService service(String secret, long minutes) {
        SecretKey key = config.jwtSecretKey(new JwtProperties(secret, minutes), new MockEnvironment());
        JwtEncoder encoder = config.jwtEncoder(key);
        return new JwtService(encoder, new JwtProperties(secret, minutes));
    }

    private JwtDecoder decoder(String secret) {
        return config.jwtDecoder(config.jwtSecretKey(new JwtProperties(secret, 5), new MockEnvironment()));
    }

    private static User user(long id, Role role) throws Exception {
        User user = new User("a@b.tn", "hash", "Test", null, role);
        Field f = User.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(user, id);
        return user;
    }

    @Test
    void tokenCarriesSubjectEmailAndRole() throws Exception {
        String token = service(SECRET, 60).issue(user(42L, Role.DRIVER)).token();

        Jwt jwt = decoder(SECRET).decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.<String>getClaim("email")).isEqualTo("a@b.tn");
        assertThat(jwt.<String>getClaim(JwtService.ROLE_CLAIM)).isEqualTo("DRIVER");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        // Issued 10 minutes ago, expired 5 minutes ago: well past the decoder's default 60 s clock skew.
        // Built by hand because JwtService never issues a token that expires before it is issued.
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("marhba").subject("1")
            .issuedAt(now.minus(Duration.ofMinutes(10)))
            .expiresAt(now.minus(Duration.ofMinutes(5)))
            .claim(JwtService.ROLE_CLAIM, Role.CUSTOMER.name())
            .build();
        SecretKey key = config.jwtSecretKey(new JwtProperties(SECRET, 5), new MockEnvironment());
        String token = config.jwtEncoder(key).encode(
            JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThatThrownBy(() -> decoder(SECRET).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() throws Exception {
        String token = service("another-secret-with-at-least-32-characters!", 60).issue(user(1L, Role.ADMIN)).token();

        assertThatThrownBy(() -> decoder(SECRET).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretIsRefused() {
        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties("too-short", 5), new MockEnvironment()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void devSecretIsRefusedInProdProfile() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");

        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties(JwtConfig.DEV_SECRET, 5), prod))
            .isInstanceOf(IllegalStateException.class);
        assertThat(config.jwtSecretKey(new JwtProperties(JwtConfig.DEV_SECRET, 5), new MockEnvironment())).isNotNull();
    }
}
