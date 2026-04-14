package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.CreateConversationRequest;
import com.chatapp.chat_backend.dto.request.LoginRequest;
import com.chatapp.chat_backend.dto.request.RegisterRequest;
import com.chatapp.chat_backend.dto.request.UpdateConversationRequest;
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

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ConversationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private String user2AccessToken;
    private String user3AccessToken;
    private Long user1Id;
    private Long user2Id;
    private Long user3Id;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Register 3 users
        user1Id = registerUser("u1_" + suffix, "u1_" + suffix + "@test.com");
        user2Id = registerUser("u2_" + suffix, "u2_" + suffix + "@test.com");
        user3Id = registerUser("u3_" + suffix, "u3_" + suffix + "@test.com");

        // Login as user1
        accessToken = loginUser("u1_" + suffix);
        // Login as user2
        user2AccessToken = loginUser("u2_" + suffix);
        // Login as user3
        user3AccessToken = loginUser("u3_" + suffix);
    }

    private String loginUser(String username) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private Long registerUser(String username, String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createConversation(Long uid1, Long uid2) throws Exception {
        CreateConversationRequest req = new CreateConversationRequest();
        req.setUserId1(uid1);
        req.setUserId2(uid2);

        MvcResult result = mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void testUpdateConversation_ReplacesParticipants_ReturnsOk() throws Exception {
        Long conversationId = createConversation(user1Id, user2Id);

        UpdateConversationRequest updateReq = new UpdateConversationRequest();
        updateReq.setParticipantIds(List.of(user1Id, user3Id));

        mockMvc.perform(put("/api/conversations/" + conversationId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId))
                .andExpect(jsonPath("$.participantUsernames").isArray());
    }

    @Test
    void testDeleteConversation_Returns204() throws Exception {
        Long conversationId = createConversation(user1Id, user2Id);

        mockMvc.perform(delete("/api/conversations/" + conversationId)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteConversation_WhenNotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/conversations/999999")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetConversation_OwnConversation_ReturnsOk() throws Exception {
        Long conversationId = createConversation(user1Id, user2Id);

        mockMvc.perform(get("/api/conversations/" + conversationId)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId))
                .andExpect(jsonPath("$.participantUsernames").isArray());
    }

    @Test
    void testGetConversation_OtherUserConversation_ReturnsForbidden() throws Exception {
        Long conversationId = createConversation(user1Id, user2Id);

        mockMvc.perform(get("/api/conversations/" + conversationId)
                .header("Authorization", "Bearer " + user3AccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateConversation_OtherUserConversation_ReturnsForbidden() throws Exception {
        Long conversationId = createConversation(user1Id, user2Id);

        UpdateConversationRequest updateReq = new UpdateConversationRequest();
        updateReq.setParticipantIds(List.of(user1Id, user3Id));

        mockMvc.perform(put("/api/conversations/" + conversationId)
                .header("Authorization", "Bearer " + user3AccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteConversation_OtherUserConversation_ReturnsForbidden() throws Exception {
        Long conversationId = createConversation(user1Id, user2Id);

        mockMvc.perform(delete("/api/conversations/" + conversationId)
                .header("Authorization", "Bearer " + user3AccessToken))
                .andExpect(status().isForbidden());
    }
}
