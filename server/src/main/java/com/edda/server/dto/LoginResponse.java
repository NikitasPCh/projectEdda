package com.edda.server.dto;

import java.util.UUID;

public record LoginResponse(UUID playerId, String username, String sessionToken) {
}