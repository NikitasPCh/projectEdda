package com.edda.server.service;

import com.edda.server.dto.LoginResponse;
import com.edda.server.entity.Player;
import com.edda.server.repository.PlayerRepository;
import com.edda.server.session.SessionTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlayerCharacterService playerCharacterService;
    private final SessionTokenStore sessionTokenStore;

    @Transactional
    public Player createPlayer(String username, String password) {
        Player player = new Player();
        player.setUsername(username);
        player.setPasswordHash(passwordEncoder.encode(password));
        Player savedPlayer = playerRepository.save(player);
        playerCharacterService.createCharacter(savedPlayer);
        return savedPlayer;
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public Optional<Player> getPlayerById(UUID id) {
        return playerRepository.findById(id);
    }

    public LoginResponse login(String username, String password) {
        Player player = playerRepository.findByUsername(username)
                .filter(p -> passwordEncoder.matches(password, p.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        String token = sessionTokenStore.issueToken(player.getId());
        return new LoginResponse(player.getId(), player.getUsername(), token);
    }
}