package com.airporttransfer.admin;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airporttransfer.AbstractIT;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AdminUserIT extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserService users;

    private User account(String prefix, Role role) {
        return users.create(prefix + "-" + UUID.randomUUID() + "@example.tn", "secret-123", prefix + " Name", null, role);
    }

    private String token(User u) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"secret-123\"}".formatted(u.getEmail())))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    @Test
    void onlyAdminsManageUsersAndSeeStats() throws Exception {
        String moderator = token(account("mod", Role.MODERATOR));
        String customer = token(account("cus", Role.CUSTOMER));
        for (String t : new String[] {moderator, customer}) {
            mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
        }
    }

    @Test
    void adminSearchesFiltersAndDisablesAccounts() throws Exception {
        User admin = account("adm", Role.ADMIN);
        String token = token(admin);
        User driver = account("zzdriver", Role.DRIVER);

        mvc.perform(get("/api/admin/users").param("q", "zzdriver").param("role", "DRIVER")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].email", hasItem(driver.getEmail())))
            .andExpect(jsonPath("$.content[0].enabled").value(true));

        mvc.perform(put("/api/admin/users/" + driver.getId()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Renamed\",\"phone\":\"+216 20 000 000\",\"role\":\"DRIVER\",\"enabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false))
            .andExpect(jsonPath("$.fullName").value("Renamed"));

        // A disabled account can no longer sign in.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"secret-123\"}".formatted(driver.getEmail())))
            .andExpect(status().isUnauthorized());

        // Admins cannot lock themselves out.
        mvc.perform(put("/api/admin/users/" + admin.getId()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Me\",\"role\":\"CUSTOMER\",\"enabled\":true}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_OWN_ACCESS"));

        mvc.perform(get("/api/admin/users/999999999").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void statsRespondsWithAllStatusesAndRejectsBadRange() throws Exception {
        String token = token(account("adm", Role.ADMIN));
        mvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.byStatus.PENDING").isNumber())
            .andExpect(jsonPath("$.byStatus.CANCELLED").isNumber());
        mvc.perform(get("/api/admin/stats").param("from", "2026-02-01T00:00:00Z").param("to", "2026-01-01T00:00:00Z")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_RANGE"));
    }
}
