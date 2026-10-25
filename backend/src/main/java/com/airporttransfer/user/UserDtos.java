package com.airporttransfer.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserDto(Long id, String email, String fullName, String phone, Role role, boolean enabled,
                          Instant createdAt) {
    }

    /** Minimal public identity of a person involved in a booking (customer, driver). */
    public record PersonDto(Long id, String fullName, String email, String phone) {
    }

    /** Public sign-up. There is deliberately no role field: this endpoint only creates customers. */
    public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 30) @Pattern(regexp = "^[+0-9 ()-]*$") String phone) {
    }

    /** Account creation by an admin (drivers, other admins, or customers). */
    public record AdminCreateUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 30) @Pattern(regexp = "^[+0-9 ()-]*$") String phone,
        @NotNull Role role) {
    }

    /** Self-service profile edit. */
    public record ProfileRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 30) @Pattern(regexp = "^[+0-9 ()-]*$") String phone) {
    }

    public record ChangePasswordRequest(
        @NotBlank @Size(max = 72) String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record ResetPasswordRequest(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    /** Admin edit of an account. The e-mail is the login and is not editable here. */
    public record AdminUpdateUserRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 30) @Pattern(regexp = "^[+0-9 ()-]*$") String phone,
        @NotNull Role role,
        @NotNull Boolean enabled) {
    }
}
