package com.edda.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "action", schema = "game")
@Getter
@Setter
@NoArgsConstructor
public class Action {

    @Id
    @Column(name = "key", length = 50)
    private String key;

    @Column(name = "skill_key", nullable = false, length = 50)
    private String skillKey;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "base_xp", nullable = false)
    private long baseXp;

    @Column(name = "reward_min", nullable = false)
    private int rewardMin;

    @Column(name = "reward_max", nullable = false)
    private int rewardMax;
}