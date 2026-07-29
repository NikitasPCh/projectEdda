package com.edda.server.dto;

import java.util.List;

public record ActionProgressResponse(String skillKey, String skillName, long xpGained, String resourceKey, String resourceName, long quantityGained, List<ItemGainResponse> itemsGained) {

    public record ItemGainResponse(String itemKey, String itemName, String rarity, int quantityGained) {
    }
}
