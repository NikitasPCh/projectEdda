# 0025. TickService as a dedicated transactional boundary for the live-tick scheduler

## Status
Accepted

## Context
`LiveTickScheduler.tick()` loops over every connected player and, per player, needs to fetch their `PlayerCharacter` (via `PlayerCharacterRepository.findByPlayerId`, pessimistically locked per [0020](0020-pessimistic-locking-for-offline-progress-calculation.md)) and compute their elapsed progress as one atomic unit — otherwise the exact lost-update race `0020` exists to prevent could recur for this caller too.

This lived in a single `calculateProgress(UUID)` overload on `PlayerCharacterService`, alongside `calculateProgress(PlayerCharacter)` — the version used by callers (`selectAction`, `getCharacterSummary`) that already hold a loaded character and shouldn't re-fetch it. Two methods sharing one name, distinguished only by parameter type, meant every call site and every search for `calculateProgress` required checking which overload was actually in play.

Splitting `calculateProgress(UUID)` into two independently-named methods — `getCharacter(UUID)` plus reuse of `calculateProgress(PlayerCharacter)` — and having `LiveTickScheduler`'s existing private `tickPlayer` helper call both directly was considered, but doesn't work, for a reason specific to how Spring's declarative transactions are implemented: `@Transactional` is enforced by a proxy wrapping the bean, and that proxy can only intercept calls arriving from *outside* the class. `tick()` calling its own private `tickPlayer` method is a plain self-invocation that never passes through any proxy — annotating `tickPlayer` `@Transactional` in place would silently do nothing at all, compiling and running fine while providing zero actual protection, exactly the kind of gap that only surfaces under real concurrent load rather than in normal testing. This holds regardless of the method's visibility modifier. Making `tick()` itself `@Transactional` instead was also rejected, since that would hold one shared transaction — and one lock — for the entire per-player sweep, rather than scoped to a single player.

## Decision
`PlayerCharacterService.calculateProgress(UUID)` is removed. `PlayerCharacterService` gains `getCharacter(UUID)` (`@Transactional`, just the locked fetch) instead; `calculateProgress(PlayerCharacter)` is unchanged.

A new class, `TickService`, is added to the `service` package — deliberately alongside `PlayerCharacterService` rather than in `websocket`, so that "every `@Transactional` method lives in the service layer," an existing pattern in this codebase that had never actually been decided on purpose until this question came up, stays true as a real decision rather than an accident of where mutating code had happened to live so far. `TickService` exists solely to give `LiveTickScheduler` a genuine cross-bean call to make: its one `@Transactional` method, `tickPlayer(UUID)`, calls `playerCharacterService.getCharacter(...)` and then `playerCharacterService.calculateProgress(...)`.

`LiveTickScheduler` no longer depends on `PlayerCharacterService` at all — only on `TickService`. `tick()` calls `tickService.tickPlayer(playerId)` (a real cross-bean call, so its `@Transactional` correctly takes effect) to get back an `Optional<ActionProgressResponse>`, then handles sending the WebSocket message itself via a small private `sendProgress` helper — that helper carries no annotation requiring proxy interception, so its being self-invoked from `tick()` is fine; the self-invocation problem only ever applies to methods Spring needs to wrap something around.

Under Spring's default `REQUIRED` propagation, `TickService.tickPlayer`'s own transaction is what `PlayerCharacterService.getCharacter` and `.calculateProgress` join when called from within it — both calls cross a real bean boundary (`TickService` → `PlayerCharacterService`), so the proxy sees them, but since a transaction is already active on the thread by the time they run, they join it rather than each opening an independent one. The pessimistic lock acquired inside `getCharacter` is therefore held continuously across both calls, for the full duration of `tickPlayer` — the same locking guarantee `0020` established, now correctly spanning two calls instead of one.

## Consequences
This codebase's `@Transactional`-lives-in-the-service-layer pattern is now an explicit decision rather than an unexamined coincidence — confirmed, in the course of making this one, that no prior ADR had actually considered or ruled on it.

`TickService` is a thin, single-method coordination class with no domain logic of its own — a legitimate but different shape of "service" than `PlayerCharacterService`, worth being clear-eyed about: it exists to solve a structural transaction-boundary problem for one specific caller, not to hold business rules.

`LiveTickScheduler` shrinks and simplifies as a side effect — it no longer needs any awareness of `PlayerCharacterService` or its locking semantics at all, only of `TickService`'s one method and the WebSocket-specific work it already owned.

The self-invocation pitfall this ADR works around — a `@Transactional` method rendered silently inert because it's only ever reached via a same-class call — applies to any future scheduled or looped per-item processing added to this codebase, not just this scheduler. Worth remembering as a general trap: adding `@Transactional` to a method is not sufficient on its own; how that method is actually *called* determines whether the annotation does anything at all.
