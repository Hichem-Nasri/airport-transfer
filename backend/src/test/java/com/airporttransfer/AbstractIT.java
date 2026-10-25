package com.airporttransfer;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class of the integration tests (run by {@code mvn verify}).
 *
 * <p>By default one PostgreSQL container (Testcontainers, needs Docker) is shared by every *IT class of the JVM.
 * Without Docker, point the tests at a local PostgreSQL with {@code TEST_DB_URL} (for example
 * {@code jdbc:postgresql://localhost:5432/airport_transfer_test}), {@code TEST_DB_USER} and {@code TEST_DB_PASSWORD}.
 * That database must be empty and disposable: Flyway creates the schema and the tests add rows with unique names.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIT {

    private static final String EXTERNAL_URL = System.getenv("TEST_DB_URL");
    private static final PostgreSQLContainer<?> POSTGRES =
        EXTERNAL_URL == null ? new PostgreSQLContainer<>("postgres:16-alpine") : null;

    static {
        if (POSTGRES != null) {
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> env("TEST_DB_USER", "airport"));
            registry.add("spring.datasource.password", () -> env("TEST_DB_PASSWORD", "airport"));
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
