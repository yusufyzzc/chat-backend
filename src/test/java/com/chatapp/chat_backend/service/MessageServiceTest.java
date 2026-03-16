package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.entity.Conversation;
import com.chatapp.chat_backend.entity.Message;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.exception.ResourceNotFoundException;
import com.chatapp.chat_backend.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private MessageService messageService;

    private User testUser;
    private Conversation testConversation;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("yusuf");

        testConversation = new Conversation();
        testConversation.setId(1L);

        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setContent("Merhaba!");
        testMessage.setSender(testUser);
        testMessage.setConversation(testConversation);
    }

    @Test
    void sendMessage_WhenValid_ReturnsMessage() {
        when(conversationService.getConversationById(1L)).thenReturn(testConversation);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        Message result = messageService.sendMessage(1L, 1L, "Merhaba!");

        assertEquals("Merhaba!", result.getContent());
        assertEquals("yusuf", result.getSender().getUsername());
    }

    @Test
    void getMessages_WhenConversationExists_ReturnsList() {
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0, 10);
        when(conversationService.getConversationById(1L)).thenReturn(testConversation);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1L, pageRequest))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(testMessage)));

        org.springframework.data.domain.Page<Message> result = messageService.getMessages(1L, pageRequest);

        assertEquals(1, result.getTotalElements());
        assertEquals("Merhaba!", result.getContent().get(0).getContent());
    }

    @Test
    void deleteMessage_WhenNotFound_ThrowsException() {
        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> messageService.deleteMessage(99L));
    }
}