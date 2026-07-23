package com.edda.server.service;

import com.edda.server.config.GameProperties;
import com.edda.server.dto.PlayerCharacterResponse;
import com.edda.server.entity.Action;
import com.edda.server.entity.ActionPrimaryReward;
import com.edda.server.entity.CharacterResource;
import com.edda.server.entity.CharacterResourceId;
import com.edda.server.entity.CharacterSkill;
import com.edda.server.entity.CharacterSkillId;
import com.edda.server.entity.Player;
import com.edda.server.entity.PlayerCharacter;
import com.edda.server.entity.Resource;
import com.edda.server.entity.Skill;
import com.edda.server.repository.ActionPrimaryRewardRepository;
import com.edda.server.repository.ActionRepository;
import com.edda.server.repository.CharacterResourceRepository;
import com.edda.server.repository.CharacterSkillRepository;
import com.edda.server.repository.PlayerCharacterRepository;
import com.edda.server.repository.ResourceRepository;
import com.edda.server.repository.SkillRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerCharacterService {

    private final PlayerCharacterRepository playerCharacterRepository;
    private final SkillRepository skillRepository;
    private final CharacterSkillRepository characterSkillRepository;
    private final ActionRepository actionRepository;
    private final GameProperties gameProperties;
    private final Random random = new Random();
    private final ResourceRepository resourceRepository;
    private final CharacterResourceRepository characterResourceRepository;
    private final ActionPrimaryRewardRepository actionPrimaryRewardRepository;

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

        List<CharacterResource> startingResources = resourceRepository.findAll().stream()
                .map(resource -> {
                    CharacterResourceId id = new CharacterResourceId();
                    id.setPlayerCharacterId(savedCharacter.getId());
                    id.setResourceKey(resource.getKey());

                    CharacterResource characterResource = new CharacterResource();
                    characterResource.setId(id);
                    characterResource.setQuantity(resource.getKey().equals("hacksilver") ? 100 : 0);
                    return characterResource;
                })
                .toList();
        characterResourceRepository.saveAll(startingResources);

        return savedCharacter;
    }

    public PlayerCharacterResponse getCharacterSummary(UUID playerId) {
        PlayerCharacter character = playerCharacterRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character not found"));

        calculateOfflineProgress(character);

        Map<String, Skill> skillsByKey = skillRepository.findAll().stream()
                .collect(Collectors.toMap(Skill::getKey, skill -> skill));

        List<PlayerCharacterResponse.SkillXpResponse> skills = characterSkillRepository.findByIdPlayerCharacterId(character.getId()).stream()
                .map(cs -> {
                    Skill skill = skillsByKey.get(cs.getId().getSkillKey());
                    return new PlayerCharacterResponse.SkillXpResponse(skill.getKey(), skill.getName(), cs.getXp());
                })
                .toList();

        Map<String, Resource> resourcesByKey = resourceRepository.findAll().stream()
                .collect(Collectors.toMap(Resource::getKey, resource -> resource));

        List<PlayerCharacterResponse.ResourceQuantityResponse> resources = characterResourceRepository.findByIdPlayerCharacterId(character.getId()).stream()
                .map(cr -> {
                    Resource resource = resourcesByKey.get(cr.getId().getResourceKey());
                    return new PlayerCharacterResponse.ResourceQuantityResponse(resource.getKey(), resource.getName(), cr.getQuantity());
                })
                .toList();

        return new PlayerCharacterResponse(character.getName(), skills, resources);
    }

    public void selectAction(UUID playerId, String actionKey) {
        PlayerCharacter character = playerCharacterRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character not found"));

        calculateOfflineProgress(character);

        Action action = actionRepository.findById(actionKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found"));

        character.setCurrentActionKey(action.getKey());
        character.setLastCalculatedAt(Instant.now());
        playerCharacterRepository.save(character);
    }

    public void calculateOfflineProgress(PlayerCharacter character) {
        if (character.getCurrentActionKey() == null) {
            return;
        }

        Duration elapsed = Duration.between(character.getLastCalculatedAt(), Instant.now());
        long n = elapsed.getSeconds() / gameProperties.actionIntervalSeconds();
        if (n == 0) {
            return;
        }
        character.setLastCalculatedAt(character.getLastCalculatedAt().plusSeconds(n * gameProperties.actionIntervalSeconds()));

        Action action = actionRepository.findById(character.getCurrentActionKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found"));

        CharacterSkillId skillId = new CharacterSkillId();
        skillId.setPlayerCharacterId(character.getId());
        skillId.setSkillKey(action.getSkillKey());

        CharacterSkill characterSkill = characterSkillRepository.findById(skillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character skill not found"));

        characterSkill.setXp(characterSkill.getXp() + action.getBaseXp() * n);
        characterSkillRepository.save(characterSkill);

        ActionPrimaryReward primaryReward = actionPrimaryRewardRepository.findById(action.getKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action primary reward not found"));

        double meanPerAction = (primaryReward.getYieldMin() + primaryReward.getYieldMax()) / 2.0;
        double variancePerAction = Math.pow(primaryReward.getYieldMax() - primaryReward.getYieldMin(), 2) / 12.0;
        double totalMean = n * meanPerAction;
        double totalVariance = n * variancePerAction;
        double totalStdDev = Math.sqrt(totalVariance);
        double sample = totalMean + random.nextGaussian() * totalStdDev;
        long yieldGained = Math.max(0, Math.round(sample));
        yieldGained = Math.min(yieldGained, (long) primaryReward.getYieldMax() * n);

        CharacterResourceId resourceId = new CharacterResourceId();
        resourceId.setPlayerCharacterId(character.getId());
        resourceId.setResourceKey(primaryReward.getResourceKey());

        CharacterResource characterResource = characterResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character resource not found"));

        characterResource.setQuantity(characterResource.getQuantity() + yieldGained);
        characterResourceRepository.save(characterResource);

        playerCharacterRepository.save(character);
    }
}