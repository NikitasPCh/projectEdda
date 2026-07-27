package com.edda.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "character_inventory", schema = "game")
@Getter
@Setter
@NoArgsConstructor
public class CharacterInventory {

    @EmbeddedId
    private CharacterInventoryId id;

    @Column(nullable = false)
    private int quantity;
}