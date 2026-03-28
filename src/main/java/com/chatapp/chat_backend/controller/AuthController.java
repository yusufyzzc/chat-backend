package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.LoginRequest;
import com.chatapp.chat_backend.dto.request.RefreshTokenRequest;
import com.chatapp.chat_backend.dto.request.RegisterRequest;
import com.chatapp.chat_backend.dto.response.AuthResponse;
import com.chatapp.chat_backend.dto.response.UserResponse;
import com.chatapp.chat_backend.entity.RefreshToken;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.security.JwtUtil;
import com.chatapp.chat_backend.service.RefreshTokenService;
import com.chatapp.chat_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userService.createUser(user);
        return ResponseEntity.ok(new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getCreatedAt()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userService.getUserByUsername(request.getUsername());
        String accessToken = jwtUtil.generateToken(request.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshToken existing = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(existing);
        String accessToken = jwtUtil.generateToken(rotated.getUser().getUsername());
        return ResponseEntity.ok(new AuthResponse(accessToken, rotated.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}