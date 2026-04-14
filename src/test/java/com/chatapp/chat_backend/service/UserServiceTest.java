package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.request.UpdateUserRequest;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.exception.BadRequestException;
import com.chatapp.chat_backend.exception.ResourceNotFoundException;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.RefreshTokenRepository;
import com.chatapp.chat_backend.repository.ConversationRepository;
import com.chatapp.chat_backend.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("yusuf");
        testUser.setEmail("yusuf@test.com");
        testUser.setPassword("123456");
    }

    @Test
    void getUserById_WhenUserExists_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(1L);

        assertEquals("yusuf", result.getUsername());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_WhenUserNotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void createUser_WhenUsernameExists_ThrowsException() {
        when(userRepository.existsByUsername("yusuf")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.createUser(testUser));
    }

    @Test
    void createUser_WhenEmailExists_ThrowsException() {
        when(userRepository.existsByUsername("yusuf")).thenReturn(false);
        when(userRepository.existsByEmail("yusuf@test.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.createUser(testUser));
    }

    @Test
    void createUser_WhenValid_SavesAndReturnsUser() {
        when(userRepository.existsByUsername("yusuf")).thenReturn(false);
        when(userRepository.existsByEmail("yusuf@test.com")).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.createUser(testUser);

        assertEquals("yusuf", result.getUsername());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void getUserByUsername_WhenNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByUsername("unknown"));
    }

    @Test
    void updateUser_WhenValid_UpdatesFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("newname")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newname");
        request.setEmail("new@test.com");

        User result = userService.updateUser(1L, request);

        assertEquals("newname", result.getUsername());
        assertEquals("new@test.com", result.getEmail());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void deleteUser_WhenUserExists_DeletesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(refreshTokenRepository).deleteByUser(testUser);
        doNothing().when(messageRepository).deleteBySenderId(1L);
        doNothing().when(conversationRepository).deleteParticipantRowsByUserId(1L);
        doNothing().when(userRepository).delete(testUser);

        userService.deleteUser(1L);

        verify(userRepository, times(1)).findById(1L);
        verify(refreshTokenRepository, times(1)).deleteByUser(testUser);
        verify(messageRepository, times(1)).deleteBySenderId(1L);
        verify(conversationRepository, times(1)).deleteParticipantRowsByUserId(1L);
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    void updateUser_WhenDuplicateUsername_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("taken");

        assertThrows(BadRequestException.class, () -> userService.updateUser(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WhenPasswordProvided_EncodesPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newSecurePass")).thenReturn("encodedPass");
        when(userRepository.save(testUser)).thenReturn(testUser);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setPassword("newSecurePass");

        User result = userService.updateUser(1L, request);

        assertEquals("encodedPass", result.getPassword());
        verify(passwordEncoder, times(1)).encode("newSecurePass");
    }
}