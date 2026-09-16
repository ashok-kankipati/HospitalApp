package com.hospital.app.controller;

import com.hospital.app.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void receptionistCannotCreateAccount() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginController.AUTHENTICATED_USER,
                new LoginResponse(true, "Login successful", "receptionist", "Receptionist", "receptionist@example.com"));

        String username = "newuser" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@example.com";
        String body = "{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\"Secret123!\",\"role\":\"Doctor\"}";

        mockMvc.perform(post("/api/auth/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
