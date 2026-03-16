package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.entity.Conversation;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.exception.ResourceNotFoundException;
import com.chatapp.chat_backend.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserService userService;

    public Conversation createConversation(Long userId1, Long userId2) {
        User user1 = userService.getUserById(userId1);
        User user2 = userService.getUserById(userId2);

        Conversation conversation = new Conversation();
        conversation.setParticipants(List.of(user1, user2));
        return conversationRepository.save(conversation);
    }

    public Conversation getConversationById(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
    }

    public Page<Conversation> getConversationsByUser(Long userId, Pageable pageable) {
        return conversationRepository.findByParticipants_Id(userId, pageable);
    }
}