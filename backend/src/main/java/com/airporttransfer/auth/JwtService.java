package com.airporttransfer.auth;

import com.airporttransfer.user.User;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues the stateless access tokens. Claims: sub = user id, email, role. */
@Service
public class JwtService {

    public static final String ROLE_CLAIM = "role";

    public record IssuedToken(String token, Instant expiresAt) {
    }

    private final JwtEncoder encoder;
    private final Duration lifetime;

    public JwtService(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.lifetime = Duration.ofMinutes(props.expirationMinutes());
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetime);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("marhba")
            .subject(String.valueOf(user.getId()))
            .issuedAt(now)
            .expiresAt(expiresAt)
            .claim("email", user.getEmail())
            .claim(ROLE_CLAIM, user.getRole().name())
            .build();
        String token = encoder.encode(
            JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
