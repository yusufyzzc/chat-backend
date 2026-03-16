package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.RegisterRequest;
import com.chatapp.chat_backend.dto.request.SendMessageRequest;
import com.chatapp.chat_backend.dto.response.UserResponse;
import com.chatapp.chat_backend.entity.Conversation;
import com.chatapp.chat_backend.repository.ConversationRepository;
import com.chatapp.chat_backend.repository.UserRepository;
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
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MessageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    private String userToken;
    private Long userId;
    private Long conversationId;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "msg_tester_" + uniqueSuffix;
        String email = "msg_tester_" + uniqueSuffix + "@example.com";

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
            userId = response.getId();
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
            Map<String, String> response = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
            userToken = response.get("token");
        }

        Conversation conversation = new Conversation();
        conversation.setParticipants(List.of(userRepository.findById(userId).get()));
        Conversation saved = conversationRepository.save(conversation);
        conversationId = saved.getId();
    }

    @Test
    void testSendMessageAndPaginate() throws Exception {
        SendMessageRequest req1 = new SendMessageRequest();
        req1.setSenderId(userId);
        req1.setContent("First message");

        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages?size=10&page=0")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("First message"))
                .andExpect(jsonPath("$.size").value(10));
    }
}
