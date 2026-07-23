INSERT INTO game.resource (key, name) VALUES ('hacksilver', 'Hacksilver');

INSERT INTO game.character_resource (player_character_id, resource_key, quantity)
SELECT id, 'hacksilver', hacksilver FROM game.player_character;

INSERT INTO game.action_primary_reward (action_key, resource_key, yield_min, yield_max)
SELECT key, 'hacksilver', hacksilver_min, hacksilver_max FROM game.action;