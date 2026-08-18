ALTER TABLE game.player_character
    ADD COLUMN pending_action_key VARCHAR(50) REFERENCES game.action(key);
