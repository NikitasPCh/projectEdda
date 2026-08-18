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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static <T> T orNotFound(Optional<T> optional, String message) {
        return optional.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, message));
    }

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

    @Transactional
    public PlayerCharacterResponse getCharacterSummary(UUID playerId) {
        PlayerCharacter character = orNotFound(playerCharacterRepository.findByPlayerId(playerId), "Character not found");

        Optional<ActionProgressResponse> progress = calculateProgress(character);

        Map<String, Skill> skillsByKey = skillRepository.findAll().stream()
                .collect(Collectors.toMap(Skill::getKey, skill -> skill));

        List<PlayerCharacterResponse.SkillXpResponse> skills = characterSkillRepository.findByIdPlayerCharacterIdOrderByIdSkillKey(character.getId()).stream()
                .map(cs -> {
                    Skill skill = skillsByKey.get(cs.getId().getSkillKey());
                    return new PlayerCharacterResponse.SkillXpResponse(skill.getKey(), skill.getName(), cs.getXp());
                })
                .toList();

        Map<String, Resource> resourcesByKey = resourceRepository.findAll().stream()
                .collect(Collectors.toMap(Resource::getKey, resource -> resource));

        List<PlayerCharacterResponse.ResourceQuantityResponse> resources = characterResourceRepository.findByIdPlayerCharacterIdOrderByIdResourceKey(character.getId()).stream()
                .map(cr -> {
                    Resource resource = resourcesByKey.get(cr.getId().getResourceKey());
                    return new PlayerCharacterResponse.ResourceQuantityResponse(resource.getKey(), resource.getName(), cr.getQuantity());
                })
                .toList();

        Map<String, Item> itemsByKey = itemRepository.findAll().stream()
                .collect(Collectors.toMap(Item::getKey, item -> item));

        List<PlayerCharacterResponse.ItemQuantityResponse> items = characterInventoryRepository.findByIdPlayerCharacterIdOrderByIdItemKey(character.getId()).stream()
                .map(ci -> {
                    Item item = itemsByKey.get(ci.getId().getItemKey());
                    return new PlayerCharacterResponse.ItemQuantityResponse(item.getKey(), item.getName(), item.getRarity().name(), ci.getQuantity());
                })
                .toList();

        String currentActionName = character.getCurrentActionKey() == null
                ? null
                : orNotFound(actionRepository.findById(character.getCurrentActionKey()), "Action not found").getName();

        String pendingActionName = character.getPendingActionKey() == null
                ? null
                : orNotFound(actionRepository.findById(character.getPendingActionKey()), "Action not found").getName();

        return new PlayerCharacterResponse(character.getName(), skills, resources, items, progress.orElse(null), character.getCurrentActionKey(), currentActionName, character.getPendingActionKey(), pendingActionName, character.getLastCalculatedAt());
    }

    @Transactional
    public void selectAction(UUID playerId, String actionKey) {
        PlayerCharacter character = orNotFound(playerCharacterRepository.findByPlayerId(playerId), "Character not found");

        Action action = orNotFound(actionRepository.findById(actionKey), "Action not found");

        if (character.getCurrentActionKey() == null) {
            character.setCurrentActionKey(action.getKey());
            character.setLastCalculatedAt(Instant.now());
        } else {
            character.setPendingActionKey(action.getKey());
        }

        playerCharacterRepository.save(character);
    }

    @Transactional
    public PlayerCharacter getCharacter(UUID playerId) {
        return orNotFound(playerCharacterRepository.findByPlayerId(playerId), "Character not found");
    }

    @Transactional
    public Optional<ActionProgressResponse> calculateProgress(PlayerCharacter character) {
        Optional<ActionProgressResponse> result = applyElapsedProgress(character);
        if (result.isPresent()) {
            playerCharacterRepository.save(character);
        }
        return result;
    }

    private Optional<ActionProgressResponse> applyElapsedProgress(PlayerCharacter character) {
        if (character.getCurrentActionKey() == null) {
            return Optional.empty();
        }

        Duration elapsed = Duration.between(character.getLastCalculatedAt(), Instant.now());
        long n = elapsed.getSeconds() / gameProperties.actionIntervalSeconds();
        if (n == 0) {
            return Optional.empty();
        }

        boolean switchingAction = character.getPendingActionKey() != null;
        long ticksToApply = switchingAction ? 1 : n;

        character.setLastCalculatedAt(character.getLastCalculatedAt().plusSeconds(ticksToApply * gameProperties.actionIntervalSeconds()));

        Action action = orNotFound(actionRepository.findById(character.getCurrentActionKey()), "Action not found");

        XpGainResult xpGainResult = applyXpGain(character, action, ticksToApply);

        String resourceKey = null;
        String resourceName = null;
        long quantityGained = 0;
        List<ActionProgressResponse.ItemGainResponse> itemsGained;

        if (action.getRewardMode() == Action.RewardMode.STANDARD) {
            PrimaryRewardResult primaryRewardResult = applyPrimaryReward(character, action, ticksToApply, 1.0);
            resourceKey = primaryRewardResult.resourceKey();
            resourceName = primaryRewardResult.resourceName();
            quantityGained = primaryRewardResult.quantityGained();
            itemsGained = rollIndependentDrops(character, action, ticksToApply);
        } else {
            itemsGained = rollWeightedPool(character, action, ticksToApply);
        }

        ActionProgressResponse result = new ActionProgressResponse(
                xpGainResult.skillKey(), xpGainResult.skillName(), xpGainResult.xpGained(),
                resourceKey, resourceName, quantityGained,
                itemsGained, action.getKey(), action.getName(), null, null);

        if (switchingAction) {
            character.setCurrentActionKey(character.getPendingActionKey());
            character.setPendingActionKey(null);
            result = applyElapsedProgress(character).orElse(result);
        }

        Action currentAction = switchingAction
                ? orNotFound(actionRepository.findById(character.getCurrentActionKey()), "Action not found")
                : action;

        String pendingActionKey = character.getPendingActionKey();
        String pendingActionName = pendingActionKey == null
                ? null
                : orNotFound(actionRepository.findById(pendingActionKey), "Action not found").getName();

        return Optional.of(new ActionProgressResponse(
                result.skillKey(), result.skillName(), result.xpGained(),
                result.resourceKey(), result.resourceName(), result.quantityGained(),
                result.itemsGained(),
                currentAction.getKey(), currentAction.getName(),
                pendingActionKey, pendingActionName));
    }

    private record XpGainResult(String skillKey, String skillName, long xpGained) {
    }

    private XpGainResult applyXpGain(PlayerCharacter character, Action action, long n) {
        CharacterSkillId skillId = new CharacterSkillId();
        skillId.setPlayerCharacterId(character.getId());
        skillId.setSkillKey(action.getSkillKey());

        CharacterSkill characterSkill = orNotFound(characterSkillRepository.findById(skillId), "Character skill not found");

        long xpGained = Math.round(action.getBaseXp() * n);
        characterSkill.setXp(characterSkill.getXp() + xpGained);
        characterSkillRepository.save(characterSkill);

        Skill skill = orNotFound(skillRepository.findById(action.getSkillKey()), "Skill not found");

        return new XpGainResult(skill.getKey(), skill.getName(), xpGained);
    }

    private record PrimaryRewardResult(String resourceKey, String resourceName, long quantityGained) {
    }

    private PrimaryRewardResult applyPrimaryReward(PlayerCharacter character, Action action, long n, double coefficient) {
        ActionPrimaryReward primaryReward = orNotFound(actionPrimaryRewardRepository.findById(action.getKey()), "Action primary reward not found");

        int range = primaryReward.getYieldMax() - primaryReward.getYieldMin() + 1;
        long quantityGained = 0;
        for (long tick = 0; tick < n; tick++) {
            quantityGained += Math.round((primaryReward.getYieldMin() + random.nextInt(range)) * coefficient);
        }

        CharacterResourceId resourceId = new CharacterResourceId();
        resourceId.setPlayerCharacterId(character.getId());
        resourceId.setResourceKey(primaryReward.getResourceKey());

        CharacterResource characterResource = orNotFound(characterResourceRepository.findById(resourceId), "Character resource not found");

        characterResource.setQuantity(characterResource.getQuantity() + quantityGained);
        characterResourceRepository.save(characterResource);

        Resource resource = orNotFound(resourceRepository.findById(primaryReward.getResourceKey()), "Resource not found");

        return new PrimaryRewardResult(resource.getKey(), resource.getName(), quantityGained);
    }

    private List<ActionProgressResponse.ItemGainResponse> rollIndependentDrops(PlayerCharacter character, Action action, long n) {
        List<ActionRareDrop> actionDrops = actionRareDropRepository.findByIdActionKey(action.getKey());
        List<ActionProgressResponse.ItemGainResponse> itemsGained = new ArrayList<>();

        for (ActionRareDrop rareDrop : actionDrops) {
            int successes = 0;
            for (long tick = 0; tick < n; tick++) {
                if (random.nextDouble() < rareDrop.getDropChance().doubleValue()) {
                    successes++;
                }
            }

            if (successes == 0) {
                continue;
            }

            itemsGained.add(creditItem(character, rareDrop.getId().getItemKey(), successes));
        }

        return itemsGained;
    }

    private List<ActionProgressResponse.ItemGainResponse> rollWeightedPool(PlayerCharacter character, Action action, long n) {
        List<ActionRareDrop> pool = actionRareDropRepository.findByIdActionKey(action.getKey());
        double totalWeight = pool.stream().mapToDouble(entry -> entry.getDropChance().doubleValue()).sum();

        Map<String, Integer> countsByItemKey = new HashMap<>();
        for (long tick = 0; tick < n; tick++) {
            double roll = random.nextDouble() * totalWeight;
            double cumulative = 0;
            for (ActionRareDrop entry : pool) {
                cumulative += entry.getDropChance().doubleValue();
                if (roll < cumulative) {
                    countsByItemKey.merge(entry.getId().getItemKey(), 1, Integer::sum);
                    break;
                }
            }
        }

        List<ActionProgressResponse.ItemGainResponse> itemsGained = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countsByItemKey.entrySet()) {
            itemsGained.add(creditItem(character, entry.getKey(), entry.getValue()));
        }

        return itemsGained;
    }

    private ActionProgressResponse.ItemGainResponse creditItem(PlayerCharacter character, String itemKey, int quantity) {
        CharacterInventoryId id = new CharacterInventoryId();
        id.setPlayerCharacterId(character.getId());
        id.setItemKey(itemKey);

        Optional<CharacterInventory> existing = characterInventoryRepository.findById(id);
        if (existing.isPresent()) {
            CharacterInventory inventory = existing.get();
            inventory.setQuantity(inventory.getQuantity() + quantity);
            characterInventoryRepository.save(inventory);
        } else {
            CharacterInventory inventory = new CharacterInventory();
            inventory.setId(id);
            inventory.setQuantity(quantity);
            characterInventoryRepository.save(inventory);
        }

        Item item = orNotFound(itemRepository.findById(itemKey), "Item not found");
        return new ActionProgressResponse.ItemGainResponse(item.getKey(), item.getName(), item.getRarity().name(), quantity);
    }
}
