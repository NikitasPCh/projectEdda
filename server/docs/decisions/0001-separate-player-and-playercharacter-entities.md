# 0001. Separate Player and PlayerCharacter entities

## Status
Accepted

## Context
Edda needs to represent two different kinds of data for each person playing the game: account-level data (username, password) used for identity/login, and game-state data used for 
actual gameplay. These two categories serve different purposes and are likely to evolve at different rates — account/auth concerns change rarely, while gameplay data will grow and change frequently as the game develops.

A single combined entity was considered, but it would tightly couple account management to gameplay mechanics, and would make it awkward to later support a player having more than one character on the same
account.

## Decision
Split the data into two entities: `Player`, holding account/identity data (`username`, `password_hash`), and `PlayerCharacter`, holding the character's core identity within the game (currently just `name`,
copied from the account's username at creation but stored independently). Gameplay-progression data, such as skill experience, lives in separate related tables rather than directly on `PlayerCharacter` —  
the reasoning for that shape is its own decision, not covered here. `PlayerCharacter` references `Player` via a `player_id` foreign key with a `UNIQUE` constraint, rather than sharing a primary key with   
`Player`. This keeps them as two independently identified entities today, while leaving a cheap path to relax the `UNIQUE` constraint later if multiple characters per account become a feature — no         
restructuring of primary keys would be needed.

## Consequences
Account and gameplay concerns are now cleanly separated, and each can evolve independently — e.g., adding auth features later doesn't touch `PlayerCharacter`, and adding new gameplay mechanics doesn't     
touch `Player`. The trade-off is that any operation needing both account and character data together requires a join across the two tables, rather than a single-table read. Supporting multiple characters  
per account later remains possible without a costly migration, since the relationship is enforced by a `UNIQUE` constraint rather than shared identity.