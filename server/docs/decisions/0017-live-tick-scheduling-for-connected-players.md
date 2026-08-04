# 0017. Live-tick scheduling for connected players

## Status
Accepted

## Context
With the WebSocket layer now able to identify which players are actively connected (`ConnectionRegistry`), the app can push progress updates to those players in real time instead of waiting for their next REST call to reveal accumulated progress. This raises a scheduling question specific to the connected case, alongside the lazy-calculation model [0009](0009-tracking-offline-progress.md) already settled for the general case.

Two approaches were considered for driving these live pushes. One scheduler per active connection, started on connect and cancelled on disconnect, mirrors each connection's own lifecycle closely, but multiplies scheduled tasks with player count and adds bookkeeping to keep each one correctly tied to its connection's lifetime. The alternative is a single shared scheduled sweep, firing on a fixed interval regardless of how many players are connected, iterating whichever players happen to be in `ConnectionRegistry` at that moment.

A related question: should each sweep credit a hardcoded "one tick's worth" of progress per firing, or reuse the existing elapsed-time-derived `n` from `calculateOfflineProgress`? Hardcoding assumes the sweep always fires exactly on schedule, an assumption that doesn't hold under real scheduler jitter, GC pauses, or slow individual iterations delaying the rest of a sweep.

A final question: should a connected player's own reward delta ever be visible to other connected players (e.g. broadcast to everyone), or strictly private to the player who earned it?

## Decision
A single shared `@Scheduled(fixedRate = 5000)` sweep iterates `ConnectionRegistry.connectedPlayerIds()` on every firing, rather than a scheduler per connection. For each connected player, it calls the existing `calculateOfflineProgress` unchanged — the same elapsed-wall-clock-time-derived `n` used by every other caller (a REST read, an action switch), never a hardcoded `n = 1` — so scheduler drift or jitter cannot cause double- or under-crediting; a late-firing sweep simply produces a larger `n` for the players it catches up on that pass.

The resulting `ActionProgressResponse`, when present, is pushed only to the WebSocket session belonging to the player who earned it (looked up via `ConnectionRegistry.get(playerId)`), serialized to JSON via Spring's autoconfigured Jackson `ObjectMapper`. Reward deltas are never broadcast to other connected players.

Per-player failures during a sweep (e.g. a session that closed in the moment between the sweep reading `connectedPlayerIds()` and the send occurring) are caught and logged per iteration, not allowed to abort the sweep for the remaining players.

## Consequences
A single scheduled task handles any number of connected players, so scheduling overhead doesn't grow with player count — only the per-tick work inside the loop does, and that work is the same `calculateOfflineProgress` call already exercised by REST endpoints, not new logic. `ConnectionRegistry` becomes the single source of truth for "who should be ticked," which the connection lifecycle (register/remove on connect/disconnect) already keeps accurate for free.

0009's lazy, elapsed-time-derived calculation remains exactly how progress is computed at every trigger point, unchanged by this decision — this ADR adds a new *trigger* (a fixed-rate sweep, for currently-connected players) alongside the existing ones (a REST read, a mutation before switching actions); it does not replace or contradict 0009's model. 0009's claim that no background scheduler is needed for offline progress remains accurate as written — it describes disconnected characters, a case this ADR doesn't touch, since this decision is scoped strictly to players who are actively connected.

Per-player failure isolation means a sweep silently tolerates individual send failures as a normal, expected condition (a disconnect race, not a bug) — this trades away a single unhandled exception that would surface a real defect immediately for every other connected player continuing to receive updates uninterrupted.
