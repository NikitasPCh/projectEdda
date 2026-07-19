package com.edda.server.service;

import com.edda.server.entity.CharacterSkill;
import com.edda.server.entity.CharacterSkillId;
import com.edda.server.entity.Player;
import com.edda.server.entity.PlayerCharacter;
import com.edda.server.repository.CharacterSkillRepository;
import com.edda.server.repository.PlayerCharacterRepository;
import com.edda.server.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerCharacterService {

    private final PlayerCharacterRepository playerCharacterRepository;
    private final SkillRepository skillRepository;
    private final CharacterSkillRepository characterSkillRepository;

    public PlayerCharacter createCharacter(Player player) {
        PlayerCharacter character = new PlayerCharacter();
        character.setPlayerId(player.getId());
        character.setName(player.getUsername());
        PlayerCharacter savedCharacter = playerCharacterRepository.save(character);

        List<CharacterSkill> startingSkills = skillRepository.findAll().stream()
                .map(skill -> {
                    CharacterSkillId id = new CharacterSkillId();
                    id.setPlayerCharacterId(savedCharacter.getId());
                    id.setSkillKey(skill.getKey());

                    CharacterSkill characterSkill = new CharacterSkill();
                    characterSkill.setId(id);
                    characterSkill.setXp(0);
                    return characterSkill;
                })
                .toList();
        characterSkillRepository.saveAll(startingSkills);

        return savedCharacter;
    }
}