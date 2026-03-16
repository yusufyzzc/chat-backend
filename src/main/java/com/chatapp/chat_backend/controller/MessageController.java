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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
                        message.getSender().getUsername(), message.getCreatedAt()));
    }

    @GetMapping
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            Pageable pageable) {
        Page<MessageResponse> messages = messageService.getMessages(conversationId, pageable)
                .map(m -> new MessageResponse(m.getId(), m.getContent(),
                        m.getSender().getUsername(), m.getCreatedAt()));
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<MessageResponse> updateMessage(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @Valid @RequestBody com.chatapp.chat_backend.dto.request.UpdateMessageRequest request) {
        Message message = messageService.updateMessage(messageId, request);
        return ResponseEntity.ok(new MessageResponse(message.getId(), message.getContent(),
                message.getSender().getUsername(), message.getCreatedAt()));
    }
}