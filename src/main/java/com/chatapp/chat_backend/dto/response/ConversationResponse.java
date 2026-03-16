package com.chatapp.chat_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ConversationResponse {
    private Long id;
    private List<String> participantUsernames;
    private LocalDateTime createdAt;
}