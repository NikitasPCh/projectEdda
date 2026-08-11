package com.edda.server.repository;

import com.edda.server.entity.PlayerCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PlayerCharacter> findByPlayerId(UUID playerId);
}