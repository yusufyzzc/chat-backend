package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.LoginRequest;
import com.chatapp.chat_backend.dto.request.RegisterRequest;
import com.chatapp.chat_backend.dto.response.UserResponse;
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
    private Long otherUserId;

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

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        if (loginResult.getResponse().getStatus() == 200) {
            userToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                    .get("accessToken").asText();
        }

        String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest otherRequest = new RegisterRequest();
        otherRequest.setUsername("otheruser_" + otherSuffix);
        otherRequest.setEmail("otheruser_" + otherSuffix + "@example.com");
        otherRequest.setPassword("password123");

        MvcResult otherResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otherRequest)))
                .andReturn();

        if (otherResult.getResponse().getStatus() == 200 || otherResult.getResponse().getStatus() == 201) {
            UserResponse response = objectMapper.readValue(otherResult.getResponse().getContentAsString(), UserResponse.class);
            otherUserId = response.getId();
        }
    }

    @Test
    void testGetAllUsersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserById_OwnProfile_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/users/" + targetUserId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void testGetUserById_OtherProfile_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users/" + otherUserId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateUser_OwnProfile_ReturnsOk() throws Exception {
        com.chatapp.chat_backend.dto.request.UpdateUserRequest request = new com.chatapp.chat_backend.dto.request.UpdateUserRequest();
        request.setEmail("updated_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");

        mockMvc.perform(put("/api/users/" + targetUserId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(request.getEmail()));
    }

    @Test
    void testUpdateUser_OtherProfile_ReturnsForbidden() throws Exception {
        com.chatapp.chat_backend.dto.request.UpdateUserRequest request = new com.chatapp.chat_backend.dto.request.UpdateUserRequest();
        request.setEmail("blocked_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");

        mockMvc.perform(put("/api/users/" + otherUserId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateUser_AsNonAdmin_ReturnsForbidden() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = new RegisterRequest();
        request.setUsername("shouldfail_" + uniqueSuffix);
        request.setEmail("shouldfail_" + uniqueSuffix + "@example.com");
        request.setPassword("password123");

        // Regular user token (not ADMIN) should get 403
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateUser_WithoutAuth_ReturnsForbidden() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = new RegisterRequest();
        request.setUsername("noauth_" + uniqueSuffix);
        request.setEmail("noauth_" + uniqueSuffix + "@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
