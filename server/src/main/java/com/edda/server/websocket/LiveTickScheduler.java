package com.edda.server.websocket;

import com.edda.server.dto.ActionProgressResponse;
import com.edda.server.service.PlayerCharacterService;
import com.edda.server.session.ConnectionRegistry;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class LiveTickScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveTickScheduler.class);

    private final ConnectionRegistry connectionRegistry;
    private final PlayerCharacterService playerCharacterService;
    private final JsonMapper objectMapper;

    public LiveTickScheduler(ConnectionRegistry connectionRegistry,
                             PlayerCharacterService playerCharacterService,
                             JsonMapper objectMapper) {
        this.connectionRegistry = connectionRegistry;
        this.playerCharacterService = playerCharacterService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 5000)
    public void tick() {
        for (UUID playerId : connectionRegistry.connectedPlayerIds()) {
            try {
                tickPlayer(playerId);
            } catch (Exception e) {
                log.warn("Failed to tick player {}", playerId, e);
            }
        }
    }

    private void tickPlayer(UUID playerId) throws IOException {
        Optional<ActionProgressResponse> progress = playerCharacterService.calculateProgress(playerId);
        if (progress.isEmpty()) {
            return;
        }

        WebSocketSession session = connectionRegistry.get(playerId);
        if (session == null || !session.isOpen()) {
            return;
        }

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(progress.get())));
    }
}