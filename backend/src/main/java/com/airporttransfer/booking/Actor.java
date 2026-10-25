package com.airporttransfer.booking;

import com.airporttransfer.user.Role;
import org.springframework.security.oauth2.jwt.Jwt;

/** Who performs an action: the signed-in user and the role carried by the token. */
public record Actor(Long userId, Role role) {

    public static Actor from(Jwt jwt) {
        return new Actor(Long.valueOf(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString("role")));
    }
}
