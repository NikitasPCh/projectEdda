package com.edda.server.dto;

import java.util.List;

public record PlayerCharacterResponse(String name, List<SkillXpResponse> skills, List<ResourceQuantityResponse> resources) {

    public record SkillXpResponse(String skillKey, String skillName, long xp) {
    }

    public record ResourceQuantityResponse(String resourceKey, String resourceName, long quantity) {
    }
}