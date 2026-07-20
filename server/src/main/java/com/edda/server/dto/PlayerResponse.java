package com.edda.server.dto;

import com.edda.server.entity.Player;

import java.util.UUID;

public record PlayerResponse(UUID id, String username) {

    public static PlayerResponse from(Player player) {
        return new PlayerResponse(player.getId(), player.getUsername());
    }
}