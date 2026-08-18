package com.edda.server.service;

import com.edda.server.dto.ActionProgressResponse;
import com.edda.server.entity.PlayerCharacter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class TickService {

    private final PlayerCharacterService playerCharacterService;

    public TickService(PlayerCharacterService playerCharacterService) {
        this.playerCharacterService = playerCharacterService;
    }

    @Transactional
    public Optional<ActionProgressResponse> tickPlayer(UUID playerId) {
        PlayerCharacter character = playerCharacterService.getCharacter(playerId);
        return playerCharacterService.calculateProgress(character);
    }
}