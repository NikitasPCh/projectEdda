INSERT INTO game.resource (key, name) VALUES
    ('wood', 'Wood');

INSERT INTO game.character_resource (player_character_id, resource_key, quantity)
    SELECT id, 'wood', 0 FROM game.player_character;

INSERT INTO game.action (key, skill_key, name, base_xp) VALUES
    ('forage', 'foraging', 'Forage', 5);

INSERT INTO game.action_primary_reward (action_key, resource_key, yield_min, yield_max) VALUES
    ('forage', 'wood', 2, 4);

INSERT INTO game.item (key, name, rarity) VALUES
    ('angelica', 'Angelica', 'uncommon'),
    ('amber', 'Amber', 'rare');

INSERT INTO game.action_rare_drop (action_key, item_key, drop_chance) VALUES
    ('forage', 'angelica', 0.0042),
    ('forage', 'amber', 0.0014);