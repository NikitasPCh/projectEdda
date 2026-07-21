ALTER TABLE game.player_character
    ADD COLUMN current_action_key VARCHAR(50) REFERENCES game.action(key),
    ADD COLUMN last_calculated_at TIMESTAMPTZ;