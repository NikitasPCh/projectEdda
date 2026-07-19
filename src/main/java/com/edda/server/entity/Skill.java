package com.edda.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "skill", schema = "game")
@Getter
@Setter
@NoArgsConstructor
public class Skill {

    @Id
    @Column(name = "key", length = 30)
    private String key;

    @Column(nullable = false, length = 50)
    private String name;
}