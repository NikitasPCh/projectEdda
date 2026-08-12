# 0022. Sliding session expiry, exempting currently-connected players

## Status
Accepted

## Context
Session tokens (`SessionTokenStore`) never expired on their own. Manual logout and single-active-session eviction (`0021`) both clean up a token, but neither helps a player who simply closes their tab, or their laptop's lid, without ever clicking "Log Out" — the token just sits in memory as a fully valid credential indefinitely, until the server process restarts. This is a real, ordinary case, not an edge case: closing just the browser tab never touches the session cookie at all (cookies are profile-scoped, not tab-scoped), and even fully quitting the browser doesn't reliably delete it either, since the cookie has no `Max-Age` and many browsers' "continue where you left off" settings resurrect cookies across a genuine quit regardless.

Two shapes of automatic expiry were weighed:

- **Fixed/absolute expiry** — a hard cutoff from issue time (e.g., 24 hours after login), regardless of activity. Rejected: the app's core mechanic is built around exactly the pattern this would punish. `calculateOfflineProgress` and the welcome-back modal exist specifically because the expected usage is "select an action, close the laptop, come back in a day or two." A fixed expiry would log out a player mid-session for using the game as designed, not for abandoning it.
- **Inactivity-based (sliding) expiry** — the clock resets on real activity, only expiring a token that's gone genuinely untouched for the full window. This targets the actual problem (an abandoned token nobody will ever come back to use) without penalizing normal usage.

Sliding expiry was chosen, but what counts as "activity" needed its own decision. The first draft defined activity narrowly — only discrete touches, i.e. `SessionTokenStore.resolve()` being called via a REST request or a WebSocket handshake. This was revised: a player who leaves the game tab open and connected, without clicking anything for an extended stretch, should not be treated as inactive. The existing `ConnectionRegistry` — built for live-tick presence, already tracking exactly which players have an open, live WebSocket connection — was the natural signal for "the tab is genuinely open right now," independent of whether the player has clicked anything recently.

A separate question was how expired tokens actually get rejected. A periodic sweep alone isn't sufficient on its own — if it only runs once an hour, a token that went stale minutes ago would still be honored as valid for the rest that hour, since nothing else would be checking it in the meantime.

## Decision
Expiry is enforced in two layers:

- **`SessionTokenStore.resolve()` is the real enforcement point.** Every authenticated request already flows through it (`SessionAuthInterceptor` for REST, `TokenHandshakeInterceptor` for the WebSocket handshake). Before honoring a token, it checks `isExpired(playerId)`: if the player has a live entry in `ConnectionRegistry`, the token is unconditionally treated as active regardless of how old it is; otherwise, it's compared against a `lastUsedAt` timestamp and a fixed `MAX_IDLE` duration (7 days, a plain constant — matching the plain-constant style already used elsewhere in this code, e.g. the forced-logout close code in `0021` — rather than an externally configurable property, since nothing else in this layer is configurable yet either). An expired token is invalidated on the spot and rejected, the same as if it had never existed.
- **A `@Scheduled` sweep on `SessionTokenStore` itself, running hourly, is pure memory hygiene** — it exists only to free map entries for tokens that are expired *and that nobody is ever going to present again*, which `resolve()` would otherwise never get a chance to reject. It reuses the exact same `isExpired` check, so there's one single definition of "expired" in the class, not two. No new class was created for this; `@Scheduled` doesn't require a dedicated orchestrator, and `SessionTokenStore` is already a `@Component`.

`lastUsedAt` is a third plain `Map<UUID, Instant>`, alongside the two existing token/playerId maps, rather than folding the timestamp into the existing reverse map's value type. A merged `Map<UUID, Session>` (bundling token and timestamp in one record) was considered — it would reduce the map count from three to two — but was rejected: `invalidate()` already relies on the two-argument `Map.remove(key, value)` to atomically avoid deleting a *newer* token if a stale/delayed invalidate call races against a fresh re-login. Reproducing that same protection against a compound record value would require a `computeIfPresent`-based conditional removal instead of a plain boolean-returning `remove`, adding real ceremony to every touch point for a change that only saves one field declaration. Three plain maps, each touched with trivial `.put()`/`.get()`/`.remove()` calls, were judged simpler overall than two maps with one compound, compute-updated value.

`SessionTokenStore` gained a direct constructor dependency on `ConnectionRegistry` — both already live in the `session` package, and there's no risk of a cycle, since `ConnectionRegistry` depends on nothing.

## Consequences
A player who leaves the game connected indefinitely never expires, no matter how long, since the connectivity check short-circuits the clock entirely — a deliberate, explicit trade-off, not an oversight: there is no absolute ceiling on session lifetime for an actively-connected player. A player who disconnects and never returns has their token rejected the moment anyone tries to use it again (most commonly via the existing session-restoration-on-load flow, which already handles a `401` by returning to the login screen — no new frontend behavior was needed), and is fully cleaned out of memory within an hour of crossing the idle threshold regardless of whether anyone ever tries.

Verified manually (no unit tests exist for `SessionTokenStore`, consistent with how the rest of this session/auth layer has been verified throughout this project): `MAX_IDLE` was temporarily dropped to 10 seconds, confirming both that a connected session survives well past that window and that a disconnected, idle one is correctly rejected on the next `resolve()` call — then reverted to `Duration.ofDays(7)` before committing.

This does not address the still-separate, longer-standing gaps: no rate-limiting on `/login`, the login timing side-channel, and the cookie's `Secure` flag being environment-conditional — all still deferred to their own future pass.
