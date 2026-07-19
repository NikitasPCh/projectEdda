package com.edda.server.repository;

import com.edda.server.entity.CharacterSkill;
import com.edda.server.entity.CharacterSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CharacterSkillRepository extends JpaRepository<CharacterSkill, CharacterSkillId> {

    List<CharacterSkill> findByIdPlayerCharacterId(UUID playerCharacterId);
}