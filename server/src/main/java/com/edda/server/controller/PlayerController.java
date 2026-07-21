package com.edda.server.controller;

import com.edda.server.dto.CreatePlayerRequest;
import com.edda.server.dto.PlayerCharacterResponse;
import com.edda.server.dto.PlayerResponse;
import com.edda.server.dto.SelectActionRequest;
import com.edda.server.entity.Player;
import com.edda.server.service.PlayerCharacterService;
import com.edda.server.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerCharacterService playerCharacterService;

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

    @GetMapping("/{playerId}/character")
    public PlayerCharacterResponse getCharacter(@PathVariable UUID playerId) {
        return playerCharacterService.getCharacterSummary(playerId);
    }

    @PostMapping("/{playerId}/character/action")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selectAction(@PathVariable UUID playerId, @RequestBody SelectActionRequest request) {
        playerCharacterService.selectAction(playerId, request.actionKey());
    }
}