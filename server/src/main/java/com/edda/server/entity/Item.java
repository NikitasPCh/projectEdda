package com.edda.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item", schema = "game")
@Getter
@Setter
@NoArgsConstructor
public class Item {

    @Id
    @Column(name = "key", length = 50)
    private String key;

    @Column(nullable = false, length = 50)
    private String name;

    public enum Rarity {
        common, uncommon, rare, epic, legendary, unique
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rarity rarity;
}
