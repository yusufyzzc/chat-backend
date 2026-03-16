package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.RegisterRequest;
import com.chatapp.chat_backend.dto.response.UserResponse;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(pageable)
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getCreatedAt()));
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt()));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        User saved = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getCreatedAt()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody com.chatapp.chat_backend.dto.request.UpdateUserRequest request) {
        User updated = userService.updateUser(id, request);
        return ResponseEntity.ok(new UserResponse(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getCreatedAt()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}