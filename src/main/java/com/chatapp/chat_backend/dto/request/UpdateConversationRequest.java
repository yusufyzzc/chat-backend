package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UpdateConversationRequest {

    @NotEmpty(message = "Participant list must not be empty")
    @Size(min = 2, message = "A conversation must have at least 2 participants")
    private List<Long> participantIds;
}
