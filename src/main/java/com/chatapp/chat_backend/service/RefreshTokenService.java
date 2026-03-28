package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.entity.RefreshToken;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.exception.BadRequestException;
import com.chatapp.chat_backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusMillis(refreshExpiration));
        token.setRevoked(false);
        return refreshTokenRepository.save(token);
    }

    public RefreshToken validateRefreshToken(String tokenStr) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (token.isRevoked()) {
            throw new BadRequestException("Refresh token has been revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token has expired");
        }
        return token;
    }

    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken old) {
        old.setRevoked(true);
        refreshTokenRepository.save(old);
        return createRefreshToken(old.getUser());
    }

    @Transactional
    public void revokeRefreshToken(String tokenStr) {
        refreshTokenRepository.findByToken(tokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }
}
