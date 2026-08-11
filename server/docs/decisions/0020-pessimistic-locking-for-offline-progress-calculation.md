# 0020. Pessimistic locking for offline-progress calculation

## Status
Accepted

## Context
`calculateOfflineProgress` reads `PlayerCharacter.lastCalculatedAt`, computes how many action ticks have elapsed, applies XP/resource/rare-drop rewards for them, and advances `lastCalculatedAt` — all inside one `@Transactional` method. This is correct for a single caller, but nothing prevented two overlapping requests from both reading the row before either had committed: each would independently compute the same elapsed window, each would apply its own reward, and whichever transaction committed last would silently overwrite the other's write rather than the two combining — a classic lost update.

This wasn't theoretical. Building a "welcome back" progress modal (shown on login, rendering the `progress` delta from `GET /players/character`) surfaced it directly: a deliberate direct fetch right after login and the dashboard's own `useQuery`-driven fetch could both reach the backend in close succession. XP, being a deterministic function of elapsed ticks, matched consistently across the two responses — but resource quantities (rolled per tick at random) sometimes differed between them, proof the same tick count was computed twice, each rolling its own independent reward, with only one write surviving.

Two ways to prevent this were considered:
- **Optimistic locking** (a `@Version` column on `PlayerCharacter`), which detects the conflict at commit time and fails the losing transaction with an exception the caller must handle.
- **Pessimistic locking** (`SELECT ... FOR UPDATE`, via `@Lock(LockModeType.PESSIMISTIC_WRITE)`), which prevents the conflict from occurring at all — a second transaction blocks until the first commits, then reads the already-updated state and correctly computes a much smaller (often zero) remaining delta.

Pessimistic locking was chosen because the "losing" request isn't a failure case at all under this approach, just a delayed read that naturally arrives at the correct answer once its wait ends. Optimistic locking would have required adding retry or error-handling logic to every current and future caller of `calculateOfflineProgress`, for a conflict expected to recur periodically — multiple entry points (a REST fetch, the live-tick scheduler, action selection) already read and write the same character row, and more are likely as the WebSocket layer grows.

## Decision
`PlayerCharacterRepository.findByPlayerId` — the single method every caller of `calculateOfflineProgress` goes through first — is annotated `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Since `getCharacterSummary`, `selectAction`, and both `calculateOfflineProgress` overloads are already `@Transactional`, the lock is held for exactly the duration of each request's transaction and released automatically on commit, with no manual lock management required anywhere.

## Consequences
Concurrent requests for the same character now serialize at this one point instead of racing — correctness is guaranteed regardless of how many code paths end up calling into offline-progress calculation concurrently in the future, not just the specific frontend timing that first surfaced the bug. The cost is a small amount of contention under genuine concurrent load, negligible at this project's scale, and scoped to the same character only — requests for different players never contend with each other.

This is deliberately paired with a frontend change that avoids firing a redundant request after login in the common case. That fix removes the wasted round-trip; this lock is what actually guarantees correctness even when a duplicate request does occur — a different tab, a retried request, or any future caller not yet anticipated.
