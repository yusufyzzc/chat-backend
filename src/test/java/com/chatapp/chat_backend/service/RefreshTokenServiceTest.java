package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.entity.RefreshToken;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.exception.BadRequestException;
import com.chatapp.chat_backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("yusuf");
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", 604800000L);
    }

    @Test
    void createRefreshToken_SavesAndReturnsToken() {
        RefreshToken saved = new RefreshToken();
        saved.setToken(UUID.randomUUID().toString());
        saved.setUser(testUser);
        saved.setExpiresAt(Instant.now().plusMillis(604800000L));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(saved);

        RefreshToken result = refreshTokenService.createRefreshToken(testUser);

        assertNotNull(result.getToken());
        assertEquals(testUser, result.getUser());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void validateRefreshToken_WhenRevoked_ThrowsBadRequest() {
        RefreshToken token = new RefreshToken();
        token.setToken("tok");
        token.setRevoked(true);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> refreshTokenService.validateRefreshToken("tok"));
    }

    @Test
    void validateRefreshToken_WhenExpired_ThrowsBadRequest() {
        RefreshToken token = new RefreshToken();
        token.setToken("tok");
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> refreshTokenService.validateRefreshToken("tok"));
    }

    @Test
    void validateRefreshToken_WhenNotFound_ThrowsBadRequest() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> refreshTokenService.validateRefreshToken("missing"));
    }

    @Test
    void rotateRefreshToken_RevokesOldAndCreatesNew() {
        RefreshToken old = new RefreshToken();
        old.setToken("old-tok");
        old.setRevoked(false);
        old.setUser(testUser);
        old.setExpiresAt(Instant.now().plusSeconds(3600));

        RefreshToken newTok = new RefreshToken();
        newTok.setToken(UUID.randomUUID().toString());
        newTok.setUser(testUser);
        newTok.setExpiresAt(Instant.now().plusMillis(604800000L));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(old).thenReturn(newTok);

        RefreshToken result = refreshTokenService.rotateRefreshToken(old);

        assertTrue(old.isRevoked());
        assertNotEquals("old-tok", result.getToken());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}
