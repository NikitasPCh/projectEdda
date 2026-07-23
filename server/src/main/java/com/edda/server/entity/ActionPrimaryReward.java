package com.edda.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "action_primary_reward", schema = "game")
@Getter
@Setter
@NoArgsConstructor
public class ActionPrimaryReward {

    @Id
    @Column(name = "action_key", length = 50)
    private String actionKey;

    @Column(name = "resource_key", nullable = false, length = 50)
    private String resourceKey;

    @Column(name = "yield_min", nullable = false)
    private int yieldMin;

    @Column(name = "yield_max", nullable = false)
    private int yieldMax;
}