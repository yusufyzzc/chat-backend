package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.CreateConversationRequest;
import com.chatapp.chat_backend.dto.request.UpdateConversationRequest;
import com.chatapp.chat_backend.dto.response.ConversationResponse;
import com.chatapp.chat_backend.entity.Conversation;
import com.chatapp.chat_backend.service.ConversationService;
import com.chatapp.chat_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final UserService userService;

    private boolean canAccessConversation(Authentication authentication, Long conversationId) {
        // Allow admins to access all conversations
        if (authentication != null) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            if (isAdmin) return true;
        }
        
        // For regular users, check if they are a participant
        if (authentication == null) return false;
        
        Conversation conversation = conversationService.getConversationById(conversationId);
        var currentUser = userService.getUserByUsername(authentication.getName());
        
        return conversation.getParticipants().stream()
                .anyMatch(p -> p.getId().equals(currentUser.getId()));
    }

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
    public ResponseEntity<ConversationResponse> getConversation(@PathVariable Long id, Authentication authentication) {
        if (!canAccessConversation(authentication, id)) {
            throw new AccessDeniedException("You don't have permission to access this conversation");
        }
        
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

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<ConversationResponse> updateConversation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConversationRequest request,
            Authentication authentication) {
        if (!canAccessConversation(authentication, id)) {
            throw new AccessDeniedException("You don't have permission to access this conversation");
        }
        
        Conversation conversation = conversationService.updateConversation(id, request);
        List<String> usernames = conversation.getParticipants().stream()
                .map(u -> u.getUsername())
                .toList();
        return ResponseEntity.ok(new ConversationResponse(conversation.getId(), usernames, conversation.getCreatedAt()));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id, Authentication authentication) {
        if (!canAccessConversation(authentication, id)) {
            throw new AccessDeniedException("You don't have permission to access this conversation");
        }
        
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}