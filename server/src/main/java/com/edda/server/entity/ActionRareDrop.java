package com.edda.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "action_rare_drop", schema = "game")
@Getter
@Setter
@NoArgsConstructor
public class ActionRareDrop {

    @EmbeddedId
    private ActionRareDropId id;

    @Column(name = "drop_chance", nullable = false, precision = 5, scale = 4)
    private BigDecimal dropChance;
}