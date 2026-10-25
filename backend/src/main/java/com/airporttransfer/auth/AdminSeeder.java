package com.airporttransfer.auth;

import com.airporttransfer.user.Role;
import com.airporttransfer.user.UserRepository;
import com.airporttransfer.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin from SEED_ADMIN_EMAIL / SEED_ADMIN_PASSWORD when both are set and no account
 * has that e-mail yet. An existing account is never modified.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserService users;
    private final UserRepository repository;
    private final String email;
    private final String password;

    public AdminSeeder(UserService users, UserRepository repository,
        @Value("${app.seed-admin.email:}") String email, @Value("${app.seed-admin.password:}") String password) {
        this.users = users;
        this.repository = repository;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.length() < 8) {
            log.warn("Admin seeding skipped: SEED_ADMIN_EMAIL is empty or SEED_ADMIN_PASSWORD is shorter than 8 characters");
            return;
        }
        if (repository.existsByEmailIgnoreCase(email.trim())) {
            return;
        }
        users.create(email, password, "Administrator", null, Role.ADMIN);
        log.info("Seeded admin account {}", email.trim());
    }
}
