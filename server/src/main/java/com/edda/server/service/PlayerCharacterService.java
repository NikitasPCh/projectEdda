package com.edda.server.service;

import com.edda.server.dto.PlayerCharacterResponse;
import com.edda.server.entity.Action;
import com.edda.server.entity.CharacterSkill;
import com.edda.server.entity.CharacterSkillId;
import com.edda.server.entity.Player;
import com.edda.server.entity.PlayerCharacter;
import com.edda.server.entity.Skill;
import com.edda.server.repository.ActionRepository;
import com.edda.server.repository.CharacterSkillRepository;
import com.edda.server.repository.PlayerCharacterRepository;
import com.edda.server.repository.SkillRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerCharacterService {

    private final PlayerCharacterRepository playerCharacterRepository;
    private final SkillRepository skillRepository;
    private final CharacterSkillRepository characterSkillRepository;
    private final ActionRepository actionRepository;

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

    public PlayerCharacterResponse getCharacterSummary(UUID playerId) {
        PlayerCharacter character = playerCharacterRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character not found"));

        Map<String, Skill> skillsByKey = skillRepository.findAll().stream()
                .collect(Collectors.toMap(Skill::getKey, skill -> skill));

        List<PlayerCharacterResponse.SkillXpResponse> skills = characterSkillRepository.findByIdPlayerCharacterId(character.getId()).stream()
                .map(cs -> {
                    Skill skill = skillsByKey.get(cs.getId().getSkillKey());
                    return new PlayerCharacterResponse.SkillXpResponse(skill.getKey(), skill.getName(), cs.getXp());
                })
                .toList();

        return new PlayerCharacterResponse(character.getName(), skills);
    }

    public void selectAction(UUID playerId, String actionKey) {
        PlayerCharacter character = playerCharacterRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character not found"));

        Action action = actionRepository.findById(actionKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found"));

        character.setCurrentActionKey(action.getKey());
        character.setLastCalculatedAt(Instant.now());
        playerCharacterRepository.save(character);
    }
}