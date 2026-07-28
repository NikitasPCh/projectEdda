package com.edda.server.repository;

import com.edda.server.entity.CharacterInventory;
import com.edda.server.entity.CharacterInventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CharacterInventoryRepository extends JpaRepository<CharacterInventory, CharacterInventoryId> {
    List<CharacterInventory> findByIdPlayerCharacterId(UUID playerCharacterId);
}