package com.chatapp.chat_backend.config;

import com.chatapp.chat_backend.entity.Role;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DefaultUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createIfNotExists("admin", "admin@test.com", "password123", Role.ADMIN);
        createIfNotExists("demo", "user@test.com", "password123", Role.USER);
    }

    private void createIfNotExists(String username, String email, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);

        log.info("Seeded default {} user: {}", role.name(), username);
    }
}