INSERT INTO game.action (key, skill_key, name, base_xp) VALUES
    ('fight_rat', 'fighting', 'Fight Rat', 5);

INSERT INTO game.action_primary_reward (action_key, resource_key, yield_min, yield_max) VALUES
    ('fight_rat', 'hacksilver', 8, 12);