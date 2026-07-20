# 0002. Multi-schema database design

## Status
Accepted

## Context
Postgres organizes tables within schemas beneath a database. All application data was initially on track to live in the default `public` schema alongside Flyway's own bookkeeping table (`flyway_schema_history`). As the app grew to have clearly distinct categories of data — account/identity data versus gameplay-state data — mixing everything into `public` would make the schema harder to navigate as more tables are added, and wouldn't reflect the conceptual separation already established at the entity level (see 0001).

## Decision
Use separate Postgres schemas to mirror the conceptual boundaries in the codebase: `public` is left as Flyway's own default/bookkeeping schema (holding `flyway_schema_history`) and isn't used for application tables; `account` holds identity/login data (currently just `player`); `game` holds gameplay-state data (`player_character`, `skill`, `character_skill`). Tables are schema-qualified directly in migration SQL (e.g., `CREATE TABLE account.player`) rather than relying on Spring/Flyway's default-schema configuration property, so each migration is explicit about where its tables live regardless of connection defaults.

## Consequences
Related tables are grouped clearly by concern, and it's obvious at a glance — both in SQL and in tools like pgAdmin4 — which category a table belongs to. Cross-schema foreign keys (e.g., `game.player_character` referencing `account.player`) work fine in Postgres, so the split doesn't complicate relationships between the two areas. The cost is a small amount of extra ceremony: schema names must be created explicitly (`CREATE SCHEMA IF NOT EXISTS ...`) and referenced in every table definition, and local database resets require dropping all schemas explicitly (tracked in `docs/db_reset.sql`) rather than a single `DROP SCHEMA public CASCADE`.
