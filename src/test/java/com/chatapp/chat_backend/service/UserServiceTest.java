package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.exception.BadRequestException;
import com.chatapp.chat_backend.exception.ResourceNotFoundException;
import com.chatapp.chat_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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
}