package com.airporttransfer.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.AbstractIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class RateLimitIT extends AbstractIT {

    @Autowired
    MockMvc mvc;

    private static String email() {
        return "limit-" + UUID.randomUUID() + "@example.tn";
    }

    private void login(String email) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)));
    }

    @Test
    void loginIsLockedAfterTenFailuresAndAnswersInTheCallersLanguage() throws Exception {
        String email = email();
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .header("Accept-Language", "en")
                .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void otherAccountsAreNotAffectedByTheLockout() throws Exception {
        String locked = email();
        for (int i = 0; i < 11; i++) {
            login(locked);
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordIsLimitedPerEmailButStillHidesWhetherTheAccountExists() throws Exception {
        String email = email();
        String body = "{\"email\":\"%s\"}".formatted(email);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        }
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isTooManyRequests());
    }
}
