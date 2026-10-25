package com.airporttransfer.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.jwt.*}: HMAC secret (32+ characters) and token lifetime. */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
