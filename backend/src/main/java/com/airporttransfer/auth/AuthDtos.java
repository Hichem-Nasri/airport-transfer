package com.airporttransfer.auth;

import com.airporttransfer.user.UserDtos.UserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {
    }

    public record AuthResponse(String token, String tokenType, Instant expiresAt, UserDto user) {
    }
}
