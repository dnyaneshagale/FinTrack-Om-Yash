package com.familyfinance;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.storage.mode=memory")
@AutoConfigureMockMvc
class AuthenticationFlowTest {
    @Autowired MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({"father,dev-father-change-me,Pappa", "son1,dev-son1-change-me,Om", "son2,dev-son2-change-me,Yash"})
    void everySeededUserCanLoginAndReadOwnSession(String username, String password, String displayName) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username)).andExpect(jsonPath("$.displayName").value(displayName)).andReturn();
        mockMvc.perform(get("/api/v1/auth/me").session((org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false))).andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username));
    }
}
