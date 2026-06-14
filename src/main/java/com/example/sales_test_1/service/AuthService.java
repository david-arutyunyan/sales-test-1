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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String SESSION_PREFIX = "session:";

    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.session.ttl:86400}")
    private long sessionTtlSeconds;

    public AuthService(UserRepository userRepository, StringRedisTemplate redis) {
        this.userRepository = userRepository;
        this.redis = redis;
        this.objectMapper = new ObjectMapper();
    }

    public AuthResponse register(RegisterRequest req) {
        log.info("Registration attempt for email: {}", req.getEmail());

        if (userRepository.existsByEmail(req.getEmail())) {
            log.warn("Registration rejected — email already exists: {}", req.getEmail());
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
        log.info("User registered: {} [{}] with role {}", user.getName(), user.getEmail(), user.getRole());

        return createSession(user);
    }

    public AuthResponse login(LoginRequest req) {
        log.info("Login attempt for email: {}", req.getEmail());

        User user = userRepository.findByEmail(req.getEmail().toLowerCase().trim())
                .orElseThrow(() -> {
                    log.warn("Login failed — no account found for: {}", req.getEmail());
                    return new IllegalArgumentException("Invalid email or password");
                });

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            log.warn("Login failed — wrong password for: {}", req.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("Login successful: {} [{}] role={}", user.getName(), user.getEmail(), user.getRole());
        return createSession(user);
    }

    public void logout(String token) {
        String key = SESSION_PREFIX + token;
        String json = redis.opsForValue().get(key);
        if (json != null) {
            try {
                SessionInfo info = objectMapper.readValue(json, SessionInfo.class);
                log.info("Logout: {} [{}]", info.getName(), info.getEmail());
            } catch (JsonProcessingException ignored) {}
        }
        redis.delete(key);
        log.debug("Session deleted: {}…", token.substring(0, 8));
    }

    public SessionInfo validateSession(String token) {
        if (token == null || token.isBlank()) return null;
        log.debug("Validating session token: {}…", token.substring(0, 8));

        String json = redis.opsForValue().get(SESSION_PREFIX + token);
        if (json == null) {
            log.debug("Session not found or expired for token: {}…", token.substring(0, 8));
            return null;
        }
        try {
            redis.expire(SESSION_PREFIX + token, sessionTtlSeconds, TimeUnit.SECONDS);
            SessionInfo info = objectMapper.readValue(json, SessionInfo.class);
            log.debug("Session valid for: {} [{}]", info.getName(), info.getEmail());
            return info;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize session for token: {}…", token.substring(0, 8), e);
            return null;
        }
    }

    private AuthResponse createSession(User user) {
        String token = UUID.randomUUID().toString();
        SessionInfo info = new SessionInfo(user.getId(), user.getEmail(), user.getName(), user.getRole());
        try {
            String json = objectMapper.writeValueAsString(info);
            redis.opsForValue().set(SESSION_PREFIX + token, json, sessionTtlSeconds, TimeUnit.SECONDS);
            log.debug("Session created for {} — token: {}… TTL={}s", user.getEmail(), token.substring(0, 8), sessionTtlSeconds);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize session for user: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to create session", e);
        }
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
