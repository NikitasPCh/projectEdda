package com.edda.server.session;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionTokenStore {

    private final Map<String, UUID> tokensToPlayerId = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerIdToToken = new ConcurrentHashMap<>();

    public String issueToken(UUID playerId) {
        String oldToken = playerIdToToken.get(playerId);
        if (oldToken != null) {
            tokensToPlayerId.remove(oldToken);
        }

        String token = UUID.randomUUID().toString();
        tokensToPlayerId.put(token, playerId);
        playerIdToToken.put(playerId, token);
        return token;
    }

    public Optional<UUID> resolve(String token) {
        return Optional.ofNullable(tokensToPlayerId.get(token));
    }

    public void invalidate(String token) {
        UUID playerId = tokensToPlayerId.remove(token);
        if (playerId != null) {
            playerIdToToken.remove(playerId, token);
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
}