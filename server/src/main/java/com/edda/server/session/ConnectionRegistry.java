package com.edda.server.session;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionRegistry {

    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(ConnectionRegistry.class);

    public void register(UUID playerId, WebSocketSession session) {
        sessions.put(playerId, session);
    }

    public void remove(UUID playerId) {
        sessions.remove(playerId);
    }

    public WebSocketSession get(UUID playerId) {
        return sessions.get(playerId);
    }

    public Set<UUID> connectedPlayerIds() {
        return sessions.keySet();
    }

    public void closeIfPresent(UUID playerId, int code, String reason) {
        WebSocketSession session = sessions.get(playerId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close(new CloseStatus(code, reason));
        } catch (IOException e) {
            log.warn("Failed to close WebSocket session for player {}", playerId, e);
        }
    }
}