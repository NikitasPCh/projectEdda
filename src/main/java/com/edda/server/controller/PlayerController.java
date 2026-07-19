package com.edda.server.controller;

import com.edda.server.dto.CreatePlayerRequest;
import com.edda.server.dto.PlayerResponse;
import com.edda.server.entity.Player;
import com.edda.server.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse createPlayer(@RequestBody CreatePlayerRequest request) {
        Player player = playerService.createPlayer(request.username(), request.password());
        return PlayerResponse.from(player);
    }

    @GetMapping
    public List<PlayerResponse> getAllPlayers() {
        return playerService.getAllPlayers().stream()
                .map(PlayerResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PlayerResponse getPlayerById(@PathVariable UUID id) {
        Player player = playerService.getPlayerById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        return PlayerResponse.from(player);
    }
}