CREATE TABLE game.action (
    key VARCHAR(50) PRIMARY KEY,
    skill_key VARCHAR(50) NOT NULL REFERENCES game.skill(key),
    name VARCHAR(50) NOT NULL,
    base_xp BIGINT NOT NULL,
    reward_min INT NOT NULL,
    reward_max INT NOT NULL
);

CREATE TABLE game.action_rare_drop (
    action_key VARCHAR(50) NOT NULL REFERENCES game.action(key),
    item_key VARCHAR(50) NOT NULL,
    drop_chance NUMERIC(5,4) NOT NULL,
    PRIMARY KEY (action_key, item_key)
);