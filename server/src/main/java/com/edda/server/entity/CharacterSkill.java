package com.edda.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "character_skill", schema = "game")
@Getter
@Setter
@NoArgsConstructor
public class CharacterSkill {

    @EmbeddedId
    private CharacterSkillId id;

    @Column(nullable = false)
    private long xp;
}