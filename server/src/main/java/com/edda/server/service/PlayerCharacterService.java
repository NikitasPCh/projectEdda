package com.edda.server.service;

import com.edda.server.config.GameProperties;
import com.edda.server.dto.ActionProgressResponse;
import com.edda.server.dto.PlayerCharacterResponse;
import com.edda.server.entity.Action;
import com.edda.server.entity.ActionPrimaryReward;
import com.edda.server.entity.ActionRareDrop;
import com.edda.server.entity.CharacterInventory;
import com.edda.server.entity.CharacterInventoryId;
import com.edda.server.entity.CharacterResource;
import com.edda.server.entity.CharacterResourceId;
import com.edda.server.entity.CharacterSkill;
import com.edda.server.entity.CharacterSkillId;
import com.edda.server.entity.Item;
import com.edda.server.entity.Player;
import com.edda.server.entity.PlayerCharacter;
import com.edda.server.entity.Resource;
import com.edda.server.entity.Skill;
import com.edda.server.repository.ActionPrimaryRewardRepository;
import com.edda.server.repository.ActionRareDropRepository;
import com.edda.server.repository.ActionRepository;
import com.edda.server.repository.CharacterInventoryRepository;
import com.edda.server.repository.CharacterResourceRepository;
import com.edda.server.repository.CharacterSkillRepository;
import com.edda.server.repository.ItemRepository;
import com.edda.server.repository.PlayerCharacterRepository;
import com.edda.server.repository.ResourceRepository;
import com.edda.server.repository.SkillRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ActionRareDropRepository actionRareDropRepository;
    private final CharacterInventoryRepository characterInventoryRepository;
    private final ItemRepository itemRepository;

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

        Optional<ActionProgressResponse> progress = calculateOfflineProgress(character);

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

        Map<String, Item> itemsByKey = itemRepository.findAll().stream()
                .collect(Collectors.toMap(Item::getKey, item -> item));

        List<PlayerCharacterResponse.ItemQuantityResponse> items = characterInventoryRepository.findByIdPlayerCharacterId(character.getId()).stream()
                .map(ci -> {
                    Item item = itemsByKey.get(ci.getId().getItemKey());
                    return new PlayerCharacterResponse.ItemQuantityResponse(item.getKey(), item.getName(), item.getRarity(), ci.getQuantity());
                })
                .toList();

        return new PlayerCharacterResponse(character.getName(), skills, resources, items, progress.orElse(null));
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

    public Optional<ActionProgressResponse> calculateOfflineProgress(PlayerCharacter character) {
        if (character.getCurrentActionKey() == null) {
            return Optional.empty();
        }

        Duration elapsed = Duration.between(character.getLastCalculatedAt(), Instant.now());
        long n = elapsed.getSeconds() / gameProperties.actionIntervalSeconds();
        if (n == 0) {
            return Optional.empty();
        }
        character.setLastCalculatedAt(character.getLastCalculatedAt().plusSeconds(n * gameProperties.actionIntervalSeconds()));

        Action action = actionRepository.findById(character.getCurrentActionKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found"));

        PrimaryRewardResult primaryRewardResult = applyPrimaryReward(character, action, n, 1.0);
        List<ActionProgressResponse.ItemGainResponse> itemsGained = rollRareDrops(character, action, n);

        playerCharacterRepository.save(character);

        return Optional.of(new ActionProgressResponse(
                primaryRewardResult.skillKey(), primaryRewardResult.skillName(), primaryRewardResult.xpGained(),
                primaryRewardResult.resourceKey(), primaryRewardResult.resourceName(), primaryRewardResult.quantityGained(),
                itemsGained));
    }

    private record PrimaryRewardResult(String skillKey, String skillName, long xpGained, String resourceKey, String resourceName, long quantityGained) {
    }

    private PrimaryRewardResult applyPrimaryReward(PlayerCharacter character, Action action, long n, double coefficient) {
        CharacterSkillId skillId = new CharacterSkillId();
        skillId.setPlayerCharacterId(character.getId());
        skillId.setSkillKey(action.getSkillKey());

        CharacterSkill characterSkill = characterSkillRepository.findById(skillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character skill not found"));

        long xpGained = Math.round(action.getBaseXp() * n * coefficient);
        characterSkill.setXp(characterSkill.getXp() + xpGained);
        characterSkillRepository.save(characterSkill);

        Skill skill = skillRepository.findById(action.getSkillKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found"));

        ActionPrimaryReward primaryReward = actionPrimaryRewardRepository.findById(action.getKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action primary reward not found"));

        int range = primaryReward.getYieldMax() - primaryReward.getYieldMin() + 1;
        long quantityGained = 0;
        for (long tick = 0; tick < n; tick++) {
            quantityGained += Math.round((primaryReward.getYieldMin() + random.nextInt(range)) * coefficient);
        }

        CharacterResourceId resourceId = new CharacterResourceId();
        resourceId.setPlayerCharacterId(character.getId());
        resourceId.setResourceKey(primaryReward.getResourceKey());

        CharacterResource characterResource = characterResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character resource not found"));

        characterResource.setQuantity(characterResource.getQuantity() + quantityGained);
        characterResourceRepository.save(characterResource);

        Resource resource = resourceRepository.findById(primaryReward.getResourceKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        return new PrimaryRewardResult(skill.getKey(), skill.getName(), xpGained, resource.getKey(), resource.getName(), quantityGained);
    }

    private List<ActionProgressResponse.ItemGainResponse> rollRareDrops(PlayerCharacter character, Action action, long n) {
        List<ActionRareDrop> actionDrops = actionRareDropRepository.findByIdActionKey(action.getKey());
        List<ActionProgressResponse.ItemGainResponse> itemsGained = new ArrayList<>();

        for (ActionRareDrop rareDrop : actionDrops) {
            int successes = 0;
            for (long j = 1; j <= n; j++) {
                if (random.nextDouble() < rareDrop.getDropChance().doubleValue()) {
                    successes++;
                }
            }

            if (successes == 0) {
                continue;
            }

            CharacterInventoryId id = new CharacterInventoryId();
            id.setPlayerCharacterId(character.getId());
            id.setItemKey(rareDrop.getId().getItemKey());

            Optional<CharacterInventory> existing = characterInventoryRepository.findById(id);
            if (existing.isPresent()) {
                CharacterInventory inventory = existing.get();
                inventory.setQuantity(inventory.getQuantity() + successes);
                characterInventoryRepository.save(inventory);
            } else {
                CharacterInventory inventory = new CharacterInventory();
                inventory.setId(id);
                inventory.setQuantity(successes);
                characterInventoryRepository.save(inventory);
            }

            Item item = itemRepository.findById(rareDrop.getId().getItemKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
            itemsGained.add(new ActionProgressResponse.ItemGainResponse(item.getKey(), item.getName(), item.getRarity(), successes));
        }

        return itemsGained;
    }
}
