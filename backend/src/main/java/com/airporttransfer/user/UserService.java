package com.airporttransfer.user;

import com.airporttransfer.common.ApiException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account rules: sign-up, admin-created accounts and credential checks. */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    /** Compared against when the e-mail is unknown, so that unknown and wrong-password logins cost the same. */
    private final String dummyHash;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    /** Public sign-up: always a CUSTOMER. */
    @Transactional
    public User register(String email, String password, String fullName, String phone) {
        return create(email, password, fullName, phone, Role.CUSTOMER);
    }

    /** Used by the admin API and by the admin seeding. */
    @Transactional
    public User create(String email, String password, String fullName, String phone, Role role) {
        String normalized = normalizeEmail(email);
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw emailUsed();
        }
        String cleanPhone = phone == null || phone.isBlank() ? null : phone.trim();
        try {
            return users.saveAndFlush(
                new User(normalized, passwordEncoder.encode(password), fullName.trim(), cleanPhone, role));
        } catch (DataIntegrityViolationException e) {
            // Two sign-ups racing on the same e-mail: the unique index decides.
            throw emailUsed();
        }
    }

    /** Returns the user when the credentials match an enabled account; otherwise INVALID_CREDENTIALS (401). */
    @Transactional(readOnly = true)
    public User authenticate(String email, String password) {
        User user = users.findByEmailIgnoreCase(normalizeEmail(email)).orElse(null);
        String hash = user == null ? dummyHash : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(password, hash);
        if (user == null || !matches || !user.isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid e-mail or password");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getActive(Long id) {
        return users.findById(id).filter(User::isEnabled)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Account not available"));
    }

    /** Profile edit by the account owner (the e-mail is the login and does not change here). */
    @Transactional
    public User updateProfile(Long id, String fullName, String phone) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        String cleanPhone = phone == null || phone.isBlank() ? null : phone.trim();
        user.applyProfile(fullName.trim(), cleanPhone);
        return users.save(user);
    }

    /** 400 CURRENT_PASSWORD_INVALID (not 401: the user is signed in, the client must not log out). */
    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("CURRENT_PASSWORD_INVALID", "The current password is incorrect");
        }
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
    }

    /** Used by the reset flow once the token has been verified. */
    @Transactional
    public void setPassword(Long id, String newPassword) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException emailUsed() {
        return ApiException.conflict("EMAIL_ALREADY_USED", "An account already exists with this e-mail");
    }
}
