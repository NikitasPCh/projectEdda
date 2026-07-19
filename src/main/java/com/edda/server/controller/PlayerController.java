package com.edda.server.controller;

import com.edda.server.dto.CreatePlayerRequest;
import com.edda.server.dto.PlayerResponse;
import com.edda.server.entity.Player;
import com.edda.server.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}