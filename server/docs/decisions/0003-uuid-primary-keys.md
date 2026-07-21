# 0003. UUID primary keys generated in application code

## Status
Accepted

## Context
Every entity needs a primary key strategy. An auto-incrementing integer ID was considered and rejected early, since sequential IDs are guessable and leak information (e.g., roughly how many players exist, or which player signed up before another). Postgres offers a database-side option to generate UUIDs automatically on insert (`DEFAULT gen_random_uuid()`), while Hibernate/JPA also supports generating UUIDs in the application layer before the insert happens (`@GeneratedValue(strategy = GenerationType.UUID)`).

## Decision
Use Hibernate-generated UUIDs (`@GeneratedValue(strategy = GenerationType.UUID)`) as the primary key strategy for all entities, rather than relying on the database's `DEFAULT gen_random_uuid()`. Migrations still declare the DB-side default as a fallback, but the application never depends on it — Hibernate always supplies the ID value itself before the row is written.

## Consequences
The generated ID is known to the application immediately after `save()` returns, without needing a round-trip to read back a database-generated value. ID generation behaves consistently regardless of how a row is inserted, since the logic lives in one place (the application) rather than being split between the database and the app. The trade-off is minor redundancy — the DB-side default exists in the schema but is effectively unused as long as all inserts go through Hibernate.
