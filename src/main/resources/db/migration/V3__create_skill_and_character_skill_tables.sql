CREATE TABLE game.skill (
    key VARCHAR(30) PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE game.character_skill (
    player_character_id UUID NOT NULL REFERENCES game.player_character(id),
    skill_key VARCHAR(30) NOT NULL REFERENCES game.skill(key),
    xp BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (player_character_id, skill_key)
);