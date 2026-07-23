package com.edda.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CharacterResourceId implements Serializable {

    @Column(name = "player_character_id")
    private UUID playerCharacterId;

    @Column(name = "resource_key")
    private String resourceKey;
}