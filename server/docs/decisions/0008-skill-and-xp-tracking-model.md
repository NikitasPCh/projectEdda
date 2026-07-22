# 0008. Skill and XP tracking model

## Status
Accepted

## Context
Characters perform multiple distinct actions (e.g., mining, combat), and each needs independent progression rather than a single combined experience value, so that a character's proficiency reflects what they've actually spent time doing. Storing XP directly as columns on `PlayerCharacter` — one column per skill — was considered, but would require a schema migration every time a new skill is added, and would make cross-skill queries (e.g., a future leaderboard by skill) awkward, comparing across columns rather than filtering/sorting rows.

## Decision
Track skills with two related tables in the `game` schema: `skill`, a small lookup/reference table listing every skill that exists in the game, keyed by a short natural string key (e.g., `'mining'`) rather than a generated UUID, since it's a small human-meaningful reference set; and `character_skill`, holding one row per (character, skill) pair with that character's `xp` in that skill. `character_skill` uses a composite primary key of `(player_character_id, skill_key)` rather than a separate surrogate `id` column plus a `UNIQUE` constraint, since nothing else needs to reference an individual `character_skill` row independently. No "level" is stored anywhere — it's intended to be computed from `xp` via a formula whenever it's needed, to avoid keeping a derived value in sync with its source.

## Consequences
Adding a new skill later requires only an `INSERT` into `game.skill` — no schema migration, no new columns, no backfilling existing character rows. Querying "all skills for a character" is efficient by design, since the composite primary key's leading column (`player_character_id`) is indexed for exactly that access pattern; querying "all characters' XP in a given skill" (e.g., for a future leaderboard) is currently less efficient, since `skill_key` is the second column of that same index, and will likely need its own dedicated index once a leaderboard feature is actually built.
