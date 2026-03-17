package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.CreateConversationRequest;
import com.chatapp.chat_backend.dto.response.ConversationResponse;
import com.chatapp.chat_backend.entity.Conversation;
import com.chatapp.chat_backend.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        Conversation conversation = conversationService.createConversation(request.getUserId1(), request.getUserId2());
        List<String> usernames = conversation.getParticipants().stream()
                .map(u -> u.getUsername())
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ConversationResponse(conversation.getId(), usernames, conversation.getCreatedAt()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getConversation(@PathVariable Long id) {
        Conversation conversation = conversationService.getConversationById(id);
        List<String> usernames = conversation.getParticipants().stream()
                .map(u -> u.getUsername())
                .toList();
        return ResponseEntity.ok(new ConversationResponse(conversation.getId(), usernames, conversation.getCreatedAt()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ConversationResponse>> getConversationsByUser(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<ConversationResponse> list = conversationService.getConversationsByUser(userId, pageable)
                .map(c -> new ConversationResponse(
                        c.getId(),
                        c.getParticipants().stream().map(u -> u.getUsername()).toList(),
                        c.getCreatedAt()));
        return ResponseEntity.ok(list);
    }
}