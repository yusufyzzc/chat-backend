package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.RegisterRequest;
import com.chatapp.chat_backend.dto.response.UserResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private Long targetUserId;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "testuser_" + uniqueSuffix;
        String email = "testuser_" + uniqueSuffix + "@example.com";

        RegisterRequest userRequest = new RegisterRequest();
        userRequest.setUsername(username);
        userRequest.setEmail(email);
        userRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andReturn();

        if (result.getResponse().getStatus() == 200 || result.getResponse().getStatus() == 201) {
            UserResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
            targetUserId = response.getId();
        }

        Map<String, String> loginRequest = Map.of(
                "username", username,
                "password", "password123"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        if (loginResult.getResponse().getStatus() == 200) {
            Map<String, String> response = objectMapper.readValue(
                    loginResult.getResponse().getContentAsString(), 
                    new TypeReference<Map<String, String>>() {}
            );
            userToken = response.get("token");
        }
    }

    @Test
    void testGetAllUsersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserByIdAfterLogin() throws Exception {
        mockMvc.perform(get("/api/users/" + targetUserId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").exists());
    }
}
