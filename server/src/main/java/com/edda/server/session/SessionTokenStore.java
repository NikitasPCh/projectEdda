package com.edda.server.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionTokenStore {

    private final Map<String, UUID> tokensToPlayerId = new ConcurrentHashMap<>();

    public String issueToken(UUID playerId) {
        String token = UUID.randomUUID().toString();
        tokensToPlayerId.put(token, playerId);
        return token;
    }

    public Optional<UUID> resolve(String token) {
        return Optional.ofNullable(tokensToPlayerId.get(token));
    }
}