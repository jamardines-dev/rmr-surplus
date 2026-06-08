package com.inventory.vehicle.server.auth;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class SessionTokenService {

    private final Map<String, SessionToken> sessions = new ConcurrentHashMap<>();

    public String createToken(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionToken(user.getId(), LocalDateTime.now()));
        return token;
    }

    public Optional<Long> findUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token)).map(SessionToken::userId);
    }

    private record SessionToken(Long userId, LocalDateTime createdAt) {
    }
}
