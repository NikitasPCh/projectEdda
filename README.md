# Project EDDA

## What is Project EDDA
A PBBG (persistent browser based game) set in the Viking age. Players choose actions to be automatically performed to gain resources, fight monsters and progress in the game.

## Status
Early development. Single player server backend so far, no client yet.

## Tech Stack
Java 21, Spring Boot, Maven, PostgreSQL, Flyway.

## Repo Structure
- `server/` — the game backend (Spring Boot / Java 21, REST API, PostgreSQL via Flyway migrations).
- `client/` — planned frontend, not yet started.

## Docs
- `server/docs/decisions` — a directory where the detailed ADR history is recorded.