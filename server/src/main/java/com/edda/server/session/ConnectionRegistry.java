package com.edda.server.session;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionRegistry {

    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();

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
}