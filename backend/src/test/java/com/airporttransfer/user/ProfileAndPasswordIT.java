package com.airporttransfer.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.AbstractIT;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class ProfileAndPasswordIT extends AbstractIT {

    /** Collects the reset events so the test can read the link that would be e-mailed. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Capture {
        final List<PasswordResetRequested> events = new ArrayList<>();

        @EventListener
        public void on(PasswordResetRequested e) {
            events.add(e);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserService users;
    @Autowired Capture capture;

    private String login(String email, String password) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    private static String email() {
        return "pw-" + UUID.randomUUID() + "@example.tn";
    }

    @Test
    void profileUpdateAndPasswordChange() throws Exception {
        String email = email();
        users.register(email, "secret-123", "Old Name", null);
        String token = login(email, "secret-123");

        mvc.perform(put("/api/auth/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\" New Name \",\"phone\":\"+33 6 12 34 56 78\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("New Name"))
            .andExpect(jsonPath("$.email").value(email));

        mvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"wrong-pass\",\"newPassword\":\"brand-new-1\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_INVALID"));
        mvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"secret-123\",\"newPassword\":\"short\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"secret-123\",\"newPassword\":\"brand-new-1\"}"))
            .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"secret-123\"}".formatted(email)))
            .andExpect(status().isUnauthorized());
        login(email, "brand-new-1");
    }

    @Test
    void forgotPasswordFlowWorksOnceAndNeverRevealsAccounts() throws Exception {
        String email = email();
        users.register(email, "secret-123", "Forgetful", null);
        capture.events.clear();

        // Unknown e-mail: same answer, nothing sent.
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody-%s@example.tn\"}".formatted(UUID.randomUUID())))
            .andExpect(status().isNoContent());
        assertThat(capture.events).isEmpty();

        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)))
            .andExpect(status().isNoContent());
        assertThat(capture.events).hasSize(1);
        String link = capture.events.get(0).link();
        String token = link.substring(link.indexOf("token=") + 6);

        String body = "{\"token\":\"%s\",\"newPassword\":\"after-reset-1\"}".formatted(token);
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNoContent());
        login(email, "after-reset-1");

        // The link works only once.
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"garbage\",\"newPassword\":\"after-reset-1\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }
}
