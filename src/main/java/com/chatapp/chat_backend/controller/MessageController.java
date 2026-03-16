package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.SendMessageRequest;
import com.chatapp.chat_backend.dto.response.MessageResponse;
import com.chatapp.chat_backend.entity.Message;
import com.chatapp.chat_backend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        Message message = messageService.sendMessage(conversationId, request.getSenderId(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse(message.getId(), message.getContent(),
                        message.getSender().getUsername(), message.getSentAt()));
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable Long conversationId) {
        List<MessageResponse> messages = messageService.getMessages(conversationId).stream()
                .map(m -> new MessageResponse(m.getId(), m.getContent(),
                        m.getSender().getUsername(), m.getSentAt()))
                .toList();
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }
}