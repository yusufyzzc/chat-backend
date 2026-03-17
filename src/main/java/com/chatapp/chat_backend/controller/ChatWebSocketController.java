package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.SendMessageRequest;
import com.chatapp.chat_backend.dto.response.MessageResponse;
import com.chatapp.chat_backend.entity.Message;
import com.chatapp.chat_backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;

    @MessageMapping("/chat.send/{conversationId}")
    @SendTo("/topic/conversation/{conversationId}")
    public MessageResponse sendMessage(
            @DestinationVariable Long conversationId,
            SendMessageRequest request) {
        Message message = messageService.sendMessage(conversationId, request.getSenderId(), request.getContent());
        return new MessageResponse(
                message.getId(),
                message.getContent(),
                message.getSender().getUsername(),
                message.getCreatedAt()
        );
    }
}
