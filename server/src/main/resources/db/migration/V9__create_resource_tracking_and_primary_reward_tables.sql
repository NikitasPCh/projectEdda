CREATE TABLE game.resource (
    key VARCHAR(50) PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE game.character_resource (
    player_character_id UUID NOT NULL REFERENCES game.player_character(id),
    resource_key VARCHAR(50) NOT NULL REFERENCES game.resource(key),
    quantity BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (player_character_id, resource_key)
);

CREATE TABLE game.action_primary_reward (
    action_key VARCHAR(50) PRIMARY KEY REFERENCES game.action(key),
    resource_key VARCHAR(50) NOT NULL REFERENCES game.resource(key),
    yield_min INT NOT NULL,
    yield_max INT NOT NULL
);