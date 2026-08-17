ALTER TABLE game.action
    ADD COLUMN reward_mode VARCHAR(20) NOT NULL DEFAULT 'STANDARD'
    CHECK (reward_mode in ('STANDARD', 'WEIGHTED_POOL'));

INSERT INTO game.action (key, skill_key, name, base_xp, reward_mode) VALUES
    ('river_fishing', 'fishing', 'River Fishing', 5, 'WEIGHTED_POOL');

INSERT INTO game.item (key, name, rarity) VALUES
    ('trout', 'Trout', 'common'),
    ('grayling', 'Grayling', 'common'),
    ('salmon', 'Salmon', 'common'),
    ('eel', 'Eel', 'common'),
    ('lamprey', 'Lamprey', 'uncommon'),
    ('pink_salmon', 'Pink Salmon', 'uncommon'),
    ('rainbow_trout', 'Rainbow Trout', 'rare'),
    ('arctic_char', 'Arctic Char', 'rare'),
    ('flounder', 'Flounder', 'rare');

INSERT INTO game.action_rare_drop (action_key, item_key, drop_chance) VALUES
    ('river_fishing', 'trout', 0.2000),
    ('river_fishing', 'grayling', 0.2000),
    ('river_fishing', 'salmon', 0.2000),
    ('river_fishing', 'eel', 0.2000),
    ('river_fishing', 'lamprey', 0.0550),
    ('river_fishing', 'pink_salmon', 0.0550),
    ('river_fishing', 'rainbow_trout', 0.0300),
    ('river_fishing', 'arctic_char', 0.0300),
    ('river_fishing', 'flounder', 0.0300);