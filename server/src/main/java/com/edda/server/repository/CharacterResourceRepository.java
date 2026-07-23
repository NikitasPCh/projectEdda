package com.edda.server.repository;

import com.edda.server.entity.CharacterResource;
import com.edda.server.entity.CharacterResourceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CharacterResourceRepository extends JpaRepository<CharacterResource, CharacterResourceId> {

    List<CharacterResource> findByIdPlayerCharacterId(UUID playerCharacterId);
}