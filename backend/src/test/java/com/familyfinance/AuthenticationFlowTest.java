package com.familyfinance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.storage.mode=memory")
@AutoConfigureMockMvc
class AuthenticationFlowTest {
    @Autowired MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({
        "father,dev-father-change-me,Pappa",
        "son1,dev-son1-change-me,Om",
        "son2,dev-son2-change-me,Yash",
        "Pappa,OmYash,Pappa",
        "Om,MummyPappa,Om",
        "Yash,MummyPappa,Yash"
    })
    void validUsersCanLogin(String username, String password, String displayName) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value(displayName));
    }

    @Test
    void invalidPasswordReturnsUnauthorized() throws Exception {
        String body = "{\"username\":\"Pappa\",\"password\":\"WrongPassword\"}";
        mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void accountsEndpointWorksWithUserHeader() throws Exception {
        mockMvc.perform(get("/api/v1/accounts").header("X-User", "father"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts").header("X-User", "son1"))
            .andExpect(status().isOk());
    }
}
