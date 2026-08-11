package com.edda.server.controller;

import com.edda.server.dto.CreatePlayerRequest;
import com.edda.server.dto.LoginRequest;
import com.edda.server.dto.LoginResponse;
import com.edda.server.dto.PlayerCharacterResponse;
import com.edda.server.dto.PlayerResponse;
import com.edda.server.dto.SelectActionRequest;
import com.edda.server.entity.Player;
import com.edda.server.service.PlayerCharacterService;
import com.edda.server.service.PlayerService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestAttribute;

import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerCharacterService playerCharacterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse createPlayer(@Valid @RequestBody CreatePlayerRequest request) {
        Player player = playerService.createPlayer(request.username(), request.password());
        return PlayerResponse.from(player);
    }

    @GetMapping("/character")
    public PlayerCharacterResponse getCharacter(@RequestAttribute UUID playerId) {
        return playerCharacterService.getCharacterSummary(playerId);
    }

    @PostMapping("/character/action")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selectAction(@RequestAttribute UUID playerId, @RequestBody SelectActionRequest request) {
        playerCharacterService.selectAction(playerId, request.actionKey());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        PlayerService.LoginResult result = playerService.login(request.username(), request.password());

        ResponseCookie cookie = ResponseCookie.from("sessionToken", result.token())
                .httpOnly(true)
                .sameSite("Lax")
                .path("/api")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(result.playerId(), result.username()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        playerService.logout(request.getCookies());

        ResponseCookie cookie = ResponseCookie.from("sessionToken", "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/api")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}