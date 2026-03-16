package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {

    @NotNull(message = "User 1 ID is required")
    private Long userId1;

    @NotNull(message = "User 2 ID is required")
    private Long userId2;
}