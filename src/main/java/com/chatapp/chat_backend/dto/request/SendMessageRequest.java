package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "Sender ID is required")
    private Long senderId;

    @NotBlank(message = "Message content cannot be empty")
    private String content;
}