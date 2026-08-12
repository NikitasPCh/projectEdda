# 0021. WebSocket close code signals forced logout, not a message envelope

## Status
Accepted

## Context
Single-active-session enforcement (`SessionTokenStore`, see the token-eviction logic added in a prior session) already invalidates a player's old session token the moment they log in again elsewhere. Without a live signal, the old tab only discovers this indirectly, the next time its `character` query refetches on window focus. The existing WebSocket layer — built for live-tick progress pushes, via `ConnectionRegistry`/`GameWebSocketHandler`/`LiveTickScheduler` — is a natural channel to notify the old tab immediately instead.

Sending that notice needed to be distinguishable from `LiveTickScheduler`'s existing progress push once a client might receive either over the same socket. The first design considered a generic envelope wrapping every message: `record WebSocketEnvelope<T>(String type, T payload)`, with `LiveTickScheduler` sending `{"type": "PROGRESS", "payload": {...}}` and the eviction path sending `{"type": "FORCED_LOGOUT", "payload": "<reason>"}`, disambiguated on the frontend by branching on `type` inside `onmessage`.

Reconsidered before writing any code: forced logout, by definition, always coincides with the connection being closed. The WebSocket close handshake already carries an application-definable status code and a short reason string as a native part of the protocol (RFC 6455 reserves `4000`–`4999` for private/application use) — there was no need to invent a message-based signal for something that's inherently a close event. Using a custom close code instead removes the need for the envelope construct entirely, not just for this one case: with forced logout traveling over the close frame, `LiveTickScheduler`'s progress push remains the *only* thing ever sent as an actual message, so no discriminator is needed there either, now or once the frontend starts consuming live-tick pushes.

## Decision
Forced logout is signaled by closing the evicted player's WebSocket session with a custom close status — code `4001`, reason `"Logged in from another location"` — rather than sending a JSON message before closing. No `WebSocketEnvelope` type or message-type discriminator was introduced anywhere.

`ConnectionRegistry` — already the class owning the raw `Map<UUID, WebSocketSession>` — gained a `closeIfPresent(UUID playerId, int code, String reason)` method, encapsulating the open-check and `IOException` handling around safely closing a player's session. `PlayerService.login`, immediately after authenticating, calls `connectionRegistry.closeIfPresent(player.getId(), FORCED_LOGOUT_CLOSE_CODE, FORCED_LOGOUT_REASON)` before issuing the new token. This keeps the *mechanism* (how to safely close a `WebSocketSession`) with the class that already owns sessions, while `PlayerService` only supplies the *reason* (a login/business-rule concern) — it needs no WebSocket-related imports at all as a result. An earlier draft had `PlayerService` reach into `ConnectionRegistry.get(...)` and close the session itself; that was reconsidered as an avoidable boundary-cross once it was clear the logic belonged on the class that already owned the session lookup, not duplicated at the one call site that needed it.

The frontend's `WebSocket.onclose` handler checks `event.code === 4001` to distinguish this from an ordinary close (its own cleanup effect calling `ws.close()`, which arrives with the default code `1000`), and reacts by resetting to the logged-out view and displaying `event.reason`.

## Consequences
Simpler on both ends: no new DTO/record type, no JSON-parsing branch on the frontend for this case, and `LiveTickScheduler` required no changes at all. The `4000`–`4999` range is reserved for exactly this kind of private application signal, so this is using a real feature of the protocol rather than working around it.

This approach only scales to signals that inherently coincide with the connection closing. If the socket ever needs to carry a second kind of out-of-band signal *without* closing the connection — a real possibility once live-tick pushes and something like chat presence share the same socket — a discriminator will become necessary at that point, and this decision should be revisited rather than assumed to still hold.
