# 0007. No eager refetch after selecting an action

## Status
Accepted

## Context
`selectActionMutation`'s `onSuccess` originally did two things: an optimistic `setQueryData` update guessing the character's new `pendingActionKey`, followed immediately by `queryClient.invalidateQueries({ queryKey: ['character', playerId] })` to reconcile that guess against the server's true state.

That eager refetch turned out to be actively harmful, not just redundant. It triggers `GET /players/character`, which calls `getCharacterSummary` on the backend — and that method independently resolves any elapsed progress as part of building an accurate response, exactly as it needs to for other callers like login (see [0024](../../server/docs/decisions/0024-queued-action-switching.md) on the server side). If enough time had already elapsed since the character's last checkpoint, this eager refetch could itself resolve a just-queued action switch prematurely, moments after the click — the same "steals the tick from the live-tick scheduler's next catch-up firing" problem that motivated removing `selectAction`'s own immediate-resolve call in the first place. The backend fix alone wasn't sufficient, because this frontend-side eager read reintroduced the identical class of bug through a different path: a visible freeze before the next tick's WebSocket update, and the "Waiting for X to start..." message flashing away almost as soon as it appeared, since the refetch's response would already show the switch as resolved.

Keeping the refetch but somehow signaling "don't resolve progress for this specific read" was considered and rejected — it would mean `getCharacterSummary` needs to distinguish this caller's intent from every other caller's, undermining its actual job of always returning accurate, up-to-date state, which login and any future caller genuinely depend on.

Given `selectAction`'s only effect, once a character already has a current action, is setting `pendingActionKey` — nothing else about the character changes as a result of the click — an optimistic update that correctly mirrors `selectAction`'s own branching logic (see the `App.jsx` fix following this decision, handling the brand-new-character case) is always accurate immediately after a successful call. There is nothing left for an eager refetch to usefully reconcile.

## Decision
`selectActionMutation`'s `onSuccess` no longer calls `invalidateQueries`. The optimistic `setQueryData` update is the only immediate feedback after a successful select-action call. Everything past that point — the switch actually resolving, `currentActionKey`/`currentActionName` updating, `pendingActionKey` clearing — happens exclusively through the next real WebSocket message pushed by the live-tick scheduler, the same mechanism that already delivers ordinary skill/resource/item tick updates.

## Consequences
This closes the freeze and message-flicker bugs that the eager refetch was causing.

A real limitation is being knowingly accepted here, not solved: this app has no automatic WebSocket reconnection logic. `ws.onclose` only handles the deliberate forced-logout case (close code `4001`); an unexpected drop — a network blip, for instance — simply leaves the socket closed, with nothing re-opening it while `view` stays `'dashboard'`. If the socket drops after a switch is queued but before the scheduler's resolving tick is ever delivered, nothing will correct the "Waiting for X to start..." display until the player manually reloads or logs back in — there is no longer an eager REST fallback to catch it in the meantime. This decision leans on that gap remaining acceptable for now; building a real reconnection strategy is separate, larger work this ADR does not attempt.

Any future mutation needing immediate UI feedback should follow this same shape — an optimistic update that precisely mirrors the server's actual branching logic, not a reflexive "invalidate and refetch" — now that eager refetching immediately after a write has demonstrated it can itself trigger unwanted server-side side effects for state that's time-sensitive.
