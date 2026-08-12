package com.edda.server.session;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionTokenStore {

    private static final Duration MAX_IDLE = Duration.ofDays(7);

    private final Map<String, UUID> tokensToPlayerId = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerIdToToken = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lastUsedAt = new ConcurrentHashMap<>();

    private final ConnectionRegistry connectionRegistry;

    public SessionTokenStore(ConnectionRegistry connectionRegistry) {
        this.connectionRegistry = connectionRegistry;
    }

    public String issueToken(UUID playerId) {
        String oldToken = playerIdToToken.get(playerId);
        if (oldToken != null) {
            tokensToPlayerId.remove(oldToken);
        }

        String token = UUID.randomUUID().toString();
        tokensToPlayerId.put(token, playerId);
        playerIdToToken.put(playerId, token);
        lastUsedAt.put(playerId, Instant.now());
        return token;
    }

    public Optional<UUID> resolve(String token) {
        UUID playerId = tokensToPlayerId.get(token);
        if (playerId == null) {
            return Optional.empty();
        }

        if (isExpired(playerId)) {
            invalidate(token);
            return Optional.empty();
        }

        lastUsedAt.put(playerId, Instant.now());
        return Optional.of(playerId);
    }

    public void invalidate(String token) {
        UUID playerId = tokensToPlayerId.remove(token);
        if (playerId != null && playerIdToToken.remove(playerId, token)) {
            lastUsedAt.remove(playerId);
        }
    }

    public void invalidateFromCookies(Cookie[] cookies) {
        extractToken(cookies).ifPresent(this::invalidate);
    }

    public Optional<UUID> resolveFromCookies(Cookie[] cookies) {
        return extractToken(cookies).flatMap(this::resolve);
    }

    private Optional<String> extractToken(Cookie[] cookies) {
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals("sessionToken"))
                .map(Cookie::getValue)
                .findFirst();
    }

    private boolean isExpired(UUID playerId) {
        if (connectionRegistry.get(playerId) != null) {
            return false;
        }
        Instant lastSeen = lastUsedAt.get(playerId);
        return lastSeen == null || Duration.between(lastSeen, Instant.now()).compareTo(MAX_IDLE) > 0;
    }

    @Scheduled(fixedRate = 3_600_000)
    public void sweepExpiredTokens() {
        for (Map.Entry<UUID, String> entry : playerIdToToken.entrySet()) {
            if (isExpired(entry.getKey())) {
                invalidate(entry.getValue());
            }
        }
    }
}