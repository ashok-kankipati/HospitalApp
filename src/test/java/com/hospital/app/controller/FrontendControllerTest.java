package com.hospital.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(FrontendController.class)
class FrontendControllerTest {
    @org.springframework.boot.test.mock.mockito.MockBean private com.hospital.app.repository.UserRepository users;
    @Autowired private MockMvc mvc;

    @Test void bothWorkspaceUrlsForwardToReactEntry() throws Exception {
        mvc.perform(get("/app")).andExpect(status().isOk()).andExpect(forwardedUrl("/app/index.html"));
        mvc.perform(get("/app/")).andExpect(status().isOk()).andExpect(forwardedUrl("/app/index.html"));
    }

    @Test void reactEntryAndWorkflowAssetsAreAvailableWithoutLogin() throws Exception {
        mvc.perform(get("/app/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"root\"")));
        mvc.perform(get("/app/workflows/js/dashboard.js")).andExpect(status().isOk())
                .andExpect(content().string(containsString("initializeHospitalDashboard")));
    }

    @Test void duoCallbackLandingPreservesItsQueryString() throws Exception {
        mvc.perform(get("/login.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/app/#/login")))
                .andExpect(content().string(containsString("window.location.search")));
    }
}
