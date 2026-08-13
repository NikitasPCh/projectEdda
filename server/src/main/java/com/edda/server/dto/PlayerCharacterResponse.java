package com.edda.server.dto;

import java.time.Instant;
import java.util.List;

public record PlayerCharacterResponse(String name, List<SkillXpResponse> skills, List<ResourceQuantityResponse> resources, List<ItemQuantityResponse> items, ActionProgressResponse progress, String currentActionKey, String currentActionName, Instant lastCalculatedAt) {

    public record SkillXpResponse(String skillKey, String skillName, long xp) {
    }

    public record ResourceQuantityResponse(String resourceKey, String resourceName, long quantity) {
    }

    public record ItemQuantityResponse(String itemKey, String itemName, String rarity, int quantity) {
    }
}