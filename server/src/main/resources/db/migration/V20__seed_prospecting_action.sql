INSERT INTO game.resource (key, name) VALUES
    ('iron_ore', 'Iron Ore');

INSERT INTO game.character_resource (player_character_id, resource_key, quantity)
    SELECT id, 'iron_ore', 0 FROM game.player_character;

INSERT INTO game.action (key, skill_key, name, base_xp) VALUES
    ('prospect', 'prospecting', 'Prospect', 5);

INSERT INTO game.action_primary_reward (action_key, resource_key, yield_min, yield_max) VALUES
    ('prospect', 'iron_ore', 1, 3);

INSERT INTO game.item (key, name, rarity) VALUES
    ('lodestone', 'Lodestone', 'uncommon'),
    ('sky_iron', 'Sky Iron', 'rare');

INSERT INTO game.action_rare_drop (action_key, item_key, drop_chance) VALUES
    ('prospect', 'lodestone', 0.0042),
    ('prospect', 'sky_iron', 0.0014);