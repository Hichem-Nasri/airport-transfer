package com.airporttransfer.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.airporttransfer.AbstractIT;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
    "app.seed-admin.email=Seed.Admin@marhba.tn", "app.seed-admin.password=seed-pass-123"})
class AdminSeederIT extends AbstractIT {

    @Autowired
    UserRepository users;
    @Autowired
    AdminSeeder seeder;

    @Test
    void adminIsSeededOnStartupAndSeedingIsIdempotent() {
        var admin = users.findByEmailIgnoreCase("seed.admin@marhba.tn").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        long before = users.count();

        ApplicationArguments none = new DefaultApplicationArguments();
        seeder.run(none);

        assertThat(users.count()).isEqualTo(before);
    }
}
