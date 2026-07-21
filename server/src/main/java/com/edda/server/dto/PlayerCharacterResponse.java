package com.edda.server.dto;

import java.util.List;

public record PlayerCharacterResponse(String name, long hacksilver, List<SkillXpResponse> skills) {

    public record SkillXpResponse(String skillKey, String skillName, long xp) {
    }
}