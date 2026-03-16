package com.chatapp.chat_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private String content;
    private String senderUsername;
    private LocalDateTime createdAt;
}