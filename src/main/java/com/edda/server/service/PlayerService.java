package com.edda.server.service;

import com.edda.server.entity.Player;
import com.edda.server.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    public Player createPlayer(String username, String password) {
        Player player = new Player();
        player.setUsername(username);
        // TODO: replace with a real hash once auth is implemented — plaintext for now
        player.setPasswordHash(password);
        return playerRepository.save(player);
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public Optional<Player> getPlayerById(UUID id) {
        return playerRepository.findById(id);
    }
}