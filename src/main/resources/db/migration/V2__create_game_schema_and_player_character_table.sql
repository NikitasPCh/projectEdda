CREATE SCHEMA IF NOT EXISTS game;

CREATE TABLE game.player_character (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL UNIQUE REFERENCES account.player(id),
    name VARCHAR(50) NOT NULL
);