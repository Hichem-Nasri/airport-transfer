package com.airporttransfer.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.AbstractIT;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AuthIT extends AbstractIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    UserService users;

    private static String email() {
        return "user-" + UUID.randomUUID() + "@example.tn";
    }

    private String body(String email, String password) {
        return """
            {"email":"%s","password":"%s","fullName":"Test User","phone":"+216 20 000 000"}
            """.formatted(email, password);
    }

    private String login(String email, String password) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(res);
        return node.get("token").asText();
    }

    @Test
    void registerCreatesCustomerAndReturnsUsableToken() throws Exception {
        String email = email();
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, "secret-123")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
            .andExpect(jsonPath("$.user.email").value(email))
            .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
            .andReturn().getResponse().getContentAsString();

        String token = json.readTree(res).get("token").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void registerIgnoresAnyRoleSentByTheClient() throws Exception {
        String email = email();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"secret-123\",\"fullName\":\"Evil\",\"role\":\"ADMIN\"}"
                    .formatted(email)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() throws Exception {
        String email = email();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(body(email, "secret-123"))).andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email.toUpperCase(), "secret-123")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
    }

    @Test
    void registerValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"fullName\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.email").value("Email"))
            .andExpect(jsonPath("$.fieldErrors.password").value("Size"))
            .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }

    @Test
    void loginSucceedsWithCorrectPasswordAndEmailInAnyCase() throws Exception {
        String email = email();
        users.register(email, "secret-123", "Test", null);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"secret-123\"}".formatted(email.toUpperCase())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() throws Exception {
        String email = email();
        users.register(email, "secret-123", "Test", null);

        for (String[] c : new String[][] {{email, "wrong-password"}, {email(), "secret-123"}}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(c[0], c[1])))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    void meRequiresAValidToken() throws Exception {
        mvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void customerCannotReachAdminOrDriverRoutes() throws Exception {
        String email = email();
        users.register(email, "secret-123", "Test", null);
        String token = login(email, "secret-123");

        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"secret-123\",\"fullName\":\"X\",\"role\":\"ADMIN\"}"
                    .formatted(email())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/driver/trips").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesDriverWhoCanLogInAndReachDriverRoutes() throws Exception {
        String adminEmail = email();
        users.create(adminEmail, "admin-pass-1", "Admin", null, Role.ADMIN);
        String adminToken = login(adminEmail, "admin-pass-1");

        String driverEmail = email();
        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"driver-pass-1\",\"fullName\":\"Driver\",\"role\":\"DRIVER\"}"
                    .formatted(driverEmail)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("DRIVER"));

        String driverToken = login(driverEmail, "driver-pass-1");
        mvc.perform(get("/api/driver/trips").header("Authorization", "Bearer " + driverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/driver/trips").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isForbidden());
    }
}
