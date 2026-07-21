package com.edda.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ActionRareDropId implements Serializable {

    @Column(name = "action_key")
    private String actionKey;

    @Column(name = "item_key")
    private String itemKey;
}