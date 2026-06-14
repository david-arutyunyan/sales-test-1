package com.example.sales_test_1.service;

import com.example.sales_test_1.dto.AuthResponse;
import com.example.sales_test_1.dto.LoginRequest;
import com.example.sales_test_1.dto.RegisterRequest;
import com.example.sales_test_1.model.SessionInfo;
import com.example.sales_test_1.model.User;
import com.example.sales_test_1.model.UserRole;
import com.example.sales_test_1.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final String SESSION_PREFIX = "session:";

    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.session.ttl:86400}")
    private long sessionTtlSeconds;

    public AuthService(UserRepository userRepository, StringRedisTemplate redis) {
        this.userRepository = userRepository;
        this.redis = redis;
        this.objectMapper = new ObjectMapper();
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        UserRole role = UserRole.CUSTOMER;
        if (req.getRole() != null) {
            try {
                role = UserRole.valueOf(req.getRole().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        User user = new User(
                null,
                req.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(req.getPassword()),
                req.getName().trim(),
                role,
                LocalDateTime.now()
        );
        user = userRepository.save(user);

        return createSession(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return createSession(user);
    }

    public void logout(String token) {
        redis.delete(SESSION_PREFIX + token);
    }

    public SessionInfo validateSession(String token) {
        if (token == null || token.isBlank()) return null;
        String json = redis.opsForValue().get(SESSION_PREFIX + token);
        if (json == null) return null;
        try {
            // Refresh TTL on active use
            redis.expire(SESSION_PREFIX + token, sessionTtlSeconds, TimeUnit.SECONDS);
            return objectMapper.readValue(json, SessionInfo.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private AuthResponse createSession(User user) {
        String token = UUID.randomUUID().toString();
        SessionInfo info = new SessionInfo(user.getId(), user.getEmail(), user.getName(), user.getRole());
        try {
            String json = objectMapper.writeValueAsString(info);
            redis.opsForValue().set(SESSION_PREFIX + token, json, sessionTtlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create session", e);
        }
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
