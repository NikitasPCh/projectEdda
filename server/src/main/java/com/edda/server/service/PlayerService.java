package com.edda.server.service;

import com.edda.server.entity.Player;
import com.edda.server.repository.PlayerRepository;
import com.edda.server.session.SessionTokenStore;
import com.edda.server.session.ConnectionRegistry;

import jakarta.servlet.http.Cookie;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlayerCharacterService playerCharacterService;
    private final SessionTokenStore sessionTokenStore;
    private final ConnectionRegistry connectionRegistry;
    private static final int FORCED_LOGOUT_CLOSE_CODE = 4001;
    private static final String FORCED_LOGOUT_REASON = "Logged in from another location";

    @Transactional
    public Player createPlayer(String username, String password) {
        Player player = new Player();
        player.setUsername(username);
        player.setPasswordHash(passwordEncoder.encode(password));
        Player savedPlayer = playerRepository.save(player);
        playerCharacterService.createCharacter(savedPlayer);
        return savedPlayer;
    }

    public record LoginResult(UUID playerId, String username, String token) {}

    public LoginResult login(String username, String password) {
        Player player = playerRepository.findByUsername(username)
                .filter(p -> passwordEncoder.matches(password, p.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        connectionRegistry.closeIfPresent(player.getId(), FORCED_LOGOUT_CLOSE_CODE, FORCED_LOGOUT_REASON);

        String token = sessionTokenStore.issueToken(player.getId());
        return new LoginResult(player.getId(), player.getUsername(), token);
    }

    public void logout(Cookie[] cookies) {
        sessionTokenStore.invalidateFromCookies(cookies);
    }
}