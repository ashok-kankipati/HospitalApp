package com.hospital.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RootRedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootRedirectsToApp() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/"));
    }

    @Test
    void appRootRedirectsToAppIndex() throws Exception {
        mockMvc.perform(get("/app"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/"));
    }
}
