package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.entity.Conversation;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.exception.ResourceNotFoundException;
import com.chatapp.chat_backend.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ConversationService conversationService;

    private User user1;
    private User user2;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("yusuf");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("ali");

        testConversation = new Conversation();
        testConversation.setId(1L);
        testConversation.setParticipants(List.of(user1, user2));
    }

    @Test
    void createConversation_WhenValid_ReturnsConversation() {
        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Conversation result = conversationService.createConversation(1L, 2L);

        assertNotNull(result);
        assertEquals(2, result.getParticipants().size());
        assertEquals("yusuf", result.getParticipants().get(0).getUsername());
        assertEquals("ali", result.getParticipants().get(1).getUsername());
        verify(conversationRepository, times(1)).save(any(Conversation.class));
    }

    @Test
    void getConversationById_WhenNotFound_ThrowsException() {
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conversationService.getConversationById(99L));
    }

    @Test
    void getConversationsByUser_ReturnsPage() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Conversation> page = new PageImpl<>(List.of(testConversation));
        when(conversationRepository.findByParticipants_Id(1L, pageRequest)).thenReturn(page);

        Page<Conversation> result = conversationService.getConversationsByUser(1L, pageRequest);

        assertEquals(1, result.getTotalElements());
        assertEquals("yusuf", result.getContent().get(0).getParticipants().get(0).getUsername());
    }
}
