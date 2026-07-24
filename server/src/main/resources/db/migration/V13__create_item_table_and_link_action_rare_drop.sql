CREATE TABLE game.item (
    key VARCHAR(50) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    rarity VARCHAR(20) NOT NULL CHECK (rarity IN ('common', 'uncommon', 'rare', 'epic', 'legendary', 'unique'))
);

ALTER TABLE game.action_rare_drop
    ADD FOREIGN KEY (item_key) REFERENCES game.item(key);
