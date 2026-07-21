# 0004. Flyway-managed schema with Hibernate ddl-auto=validate

## Status
Accepted

## Context
Spring Data JPA can manage the database schema automatically (`spring.jpa.hibernate.ddl-auto=update`), inferring and applying table changes from entity classes. This is convenient for prototyping but doesn't produce a reviewable, versioned history of schema changes, and Hibernate's automatic schema updates aren't intended for long-term use. Since Edda is a persistent game where schema correctness and evolution history matter, an explicit migration tool was preferred over letting Hibernate infer the schema.

## Decision
Use Flyway to manage all schema changes via versioned SQL migration files (`V1__..sql`, `V2__..sql`, etc.) under `src/main/resources/db/migration`. Hibernate's `ddl-auto` is set to `validate`, meaning Hibernate only checks that entity mappings match the schema Flyway already created, and never generates or alters schema itself. Note: Spring Boot 4 splits Flyway's Spring integration into its own `spring-boot-starter-flyway` artifact, separate from the raw `flyway-core` dependency — both are required for Flyway's autoconfiguration to activate.

## Consequences
Every schema change is captured as an explicit, reviewable SQL file with its own place in version control history, rather than being inferred implicitly. Hibernate acts purely as a safety check, catching cases where entity mappings and the actual schema have drifted apart, rather than silently working around them. The cost is more manual effort per schema change and Flyway's immutability rule: once a migration has been applied, its content can never be edited — even a comment-only change — without triggering a checksum mismatch, requiring a full local reset during early development.
