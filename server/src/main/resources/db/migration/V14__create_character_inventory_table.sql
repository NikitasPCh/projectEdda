CREATE TABLE game.character_inventory (
    player_character_id UUID NOT NULL REFERENCES game.player_character(id),
    item_key VARCHAR(50) NOT NULL REFERENCES game.item(key),
    quantity INT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (player_character_id, item_key)
);