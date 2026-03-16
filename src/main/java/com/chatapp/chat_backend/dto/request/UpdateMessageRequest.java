package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMessageRequest {

    @NotBlank(message = "Message content cannot be blank")
    private String content;

}
