# 0019. Session-derived identity for protected endpoints

## Status
Accepted

## Context
`GET /players/{playerId}/character` and `POST /players/{playerId}/character/action` trust a bare `{playerId}` path variable with no check at all against the caller's session. This was a known, deliberately deferred gap from the earlier login/WebSocket auth work (see `0018`) — REST auth was explicitly scoped out as "a separate future decision" once the cookie-based session transport was designed. With the React frontend now performing real logins and about to build its first dashboard data fetch against `getCharacter`, that gap stops being hypothetical: the very first real request the dashboard makes would let any caller read or act on any player's character just by supplying a UUID, logged in or not.

Two ways to close this were considered:

- **Verify-against**: keep `{playerId}` in the URL, and add a check in each endpoint (or a shared interceptor) that the identity resolved from the session cookie matches the path variable, rejecting on mismatch.
- **Derive-only**: drop `{playerId}` from these URLs entirely. The server resolves the caller's identity purely from the `sessionToken` cookie; the client never supplies a player identity on these endpoints at all.

Verify-against still leaves a client-supplied identity claim in the URL — the request is only as safe as the correctness of the comparison check, which must be present and correct on every current and future endpoint that touches player-scoped data. A forgotten check on a new endpoint silently reopens the same hole. Derive-only removes the claim itself: there is no path segment for a mismatch check to get right or wrong, because the server never reads player identity from anything the client sends.

This also mirrors the pattern already established for the WebSocket handshake: `TokenHandshakeInterceptor` resolves `playerId` from the session cookie via `SessionTokenStore` and never trusts a client-supplied ID for identity, even though nothing forced that WebSocket-specific design to also apply to REST.

## Decision
`GET /players/{playerId}/character` and `POST /players/{playerId}/character/action` are restructured to `GET /players/character` and `POST /players/character/action`, dropping the path variable entirely. A `HandlerInterceptor` (structurally parallel to `TokenHandshakeInterceptor`) resolves the `sessionToken` cookie via `SessionTokenStore` before these endpoints run, rejecting with `401` if the cookie is missing or doesn't resolve. On success it stores the resolved `playerId` as a request attribute, which the controller methods read via `@RequestAttribute UUID playerId` instead of `@PathVariable`.

`@RequestAttribute` is required by default, the same as `@PathVariable` — if the interceptor is ever misconfigured for a given endpoint (wrong `addPathPatterns`, a new endpoint added later without being scoped in), the attribute simply won't exist and Spring fails the request with a `500` rather than silently proceeding with a missing or null identity. This is a fail-closed default: a wiring mistake breaks loudly instead of quietly reintroducing the hole this decision closes.

## Consequences
Player identity for these two endpoints can no longer be spoofed or guessed via the URL — the only source of truth is server-side session state resolved from the `HttpOnly` cookie already established by `0018`. Any frontend calling these endpoints must rely on an active session; there is no longer a way to address "some player's" data directly by ID.

This decision covers only the two character/action endpoints. It does not address `GET /players` (`getAllPlayers`), which still returns every player with no auth check and a broader blast radius (no UUID guessing required at all) — left as a separate, still-open decision. It also does not address the other previously-flagged login-step gaps (timing side-channel on username lookup, no rate-limiting on `/login`, the session cookie's `Secure` flag not yet being environment-conditional) or token expiry/logout, all of which remain open and undocumented pending their own decisions.
