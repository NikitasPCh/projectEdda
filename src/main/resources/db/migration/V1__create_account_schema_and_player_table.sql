CREATE SCHEMA IF NOT EXISTS account;

CREATE TABLE account.player (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);