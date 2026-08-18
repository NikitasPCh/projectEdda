# 0024. Queued action switching to avoid tick desync and cross-action misattribution

## Status
Accepted

## Context
`selectAction` originally reset `PlayerCharacter.lastCalculatedAt` to `Instant.now()` at the moment of switching. This cleanly discarded any fractional leftover time under the old action, but also knocked the character's checkpoint out of phase with the live-tick scheduler's fixed-rate rhythm ([0017](0017-live-tick-scheduling-for-connected-players.md)) — `Instant.now()` at click time has no relationship to the scheduler's own absolute firing schedule. The visible symptom was a delay after switching, up to just under one scheduler interval, before the new action's first tick registered: the scheduler's next firing wouldn't necessarily land soon enough after the newly-reset checkpoint to catch it.

Two alternatives were tried and rejected before landing on the design below:

- **Simply not resetting `lastCalculatedAt` at all.** This preserves phase alignment with the scheduler perfectly, but reopens a correctness problem this project had already deliberately closed: a leftover fractional-tick remainder, genuinely earned under the *old* action, would end up folded into the *new* action's next credited tick once enough further time passed — misattributing a few seconds of one action's reward to another every single switch.
- **Resolving eagerly, synchronously, at the moment of the switch request** (whether from `selectAction` itself, or implicitly via an eager follow-up REST read right after). This was tried and specifically reverted: only the live-tick scheduler's own resolution is ever announced to a connected client, via a WebSocket push — any resolution triggered by a REST call is silent. Resolving early "steals" the elapsed time the scheduler's very next firing would otherwise have found, leaving that firing with nothing to credit and deferring the actual WS-announced, visible update to a *later* firing than necessary. For a connected player, eager resolution made the visible freeze worse, not better, than simply leaving the switch queued for the scheduler to find naturally.

## Decision
`player_character` gains a nullable `pending_action_key` column (`V21`, FK to `game.action`), alongside the existing `current_action_key`.

`selectAction`: a character with no current action yet still switches immediately (there is no in-progress cycle to protect). Otherwise, it only ever sets `pendingActionKey` — it never touches `lastCalculatedAt`, and never itself attempts to resolve anything.

`applyElapsedProgress` — the single, already-existing elaped-time-derived calculation every caller (a REST read, a live-tick sweep) already funnels through — caps how many ticks get credited to the *current* action at exactly `1` whenever a pending switch exists, regardless of how much time has actually elapsed. It then performs the swap (`currentActionKey` becomes the pending key, `pendingActionKey` cleared) and recurses on itself to credit any further elapsed time under the newly-current action. This guarantees the old action is always credited a complete, un-misattributed final tick before the new one's clock starts, and the new action's clock starts already in phase with the scheduler's rhythm, since `lastCalculatedAt` only ever advances by whole-tick increments from its own prior value — it is never reset to "now" anywhere in this flow.

Nothing proactively resolves a pending switch outside of this same, single, elapsed-time calculation. It resolves whenever something that already legitimately needs to know elapsed progress happens to run — the live-tick scheduler for a connected player (which always bundles resolution with the WS announcement, so the swap and the visible bar reset land at the same moment), or a future request from anyone else, connected or not.

## Consequences
A one-time delay before the first tick after a switch takes effect is expected and bounded — governed by the same scheduler-interval-plus-per-character-catch-up-offset that already governs ordinary ticking, nothing switching-specific about it. Shrinking that bound further (e.g. a smaller scheduler interval) is a separate, orthogonal decision, not something this design attempts to solve.

`ActionProgressResponse` gained `currentActionKey`/`currentActionName`/`pendingActionKey`/`pendingActionName`, re-read fresh from the character at the very end of `applyElapsedProgress` rather than reused from any variable computed earlier in the method — necessary because the recursive swap can change what's true partway through a single call, and every caller (REST or WS) needs the character's actual current state, not a stale mid-method snapshot.

One narrow, deliberately accepted trade-off: when a switch resolves with no leftover elapsed time to also credit toward the new action (the common case for a connected player), the reward figures in that response describe the *old* action's closing tick, while `currentActionKey` in the same response already reflects the *new* action — two true facts about different moments, reported together. Judged acceptable rather than worth a larger, lossless response-shape redesign, since the underlying data is always correct; only the narration of "what earned this" versus "what's current now" can momentarily diverge.

This also sets a real constraint on future code, not just a historical note: no caller may eagerly resolve elapsed progress purely as a side effect of an unrelated request (an eager follow-up read right after a write, say). Doing so can silently steal a tick from the scheduler's next catch-up firing, delaying its WS-announced update to a later firing than necessary — this was tried twice during this design's development (once in `selectAction` itself, once via an automatic frontend refetch immediately following it) and reverted both times for exactly this reason.
