INSERT INTO game.item (key, name, rarity) VALUES
    ('rune_carved_bone', 'Rune-Carved Bone', 'uncommon'),
    ('grave_coins', 'Grave Coins', 'rare');

INSERT INTO game.action_rare_drop (action_key, item_key, drop_chance) VALUES
    ('fight_rat', 'rune_carved_bone', 0.0042),
    ('fight_rat', 'grave_coins', 0.0014);
