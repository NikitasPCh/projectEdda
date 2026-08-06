# 0018. HttpOnly cookie session transport

## Status
Accepted

## Context
The login endpoint (`POST /players/login`) currently returns the session token as a plain field in the `LoginResponse` JSON body. With the React frontend now making real login calls instead of the placeholder it started with, the token needs somewhere to live on the client between requests, and that choice needs to be made before any frontend login code is written rather than retrofitted afterward.

Three options were considered. Keeping the token only in React component state (plain in-memory JS) is simplest but loses the session on every page refresh, forcing a re-login far more often than is reasonable. Storing it in `localStorage` survives refreshes, but offers no real security improvement over plain memory — any script able to run in the page (e.g. via an XSS vector) can read `localStorage` just as freely as a JS variable, so it's convenience-only, not a security upgrade. An `HttpOnly` cookie, set by the server and never exposed to JavaScript at all, is immune to that class of token theft — client-side script cannot read it even if malicious script is running on the page.

Since a cookie-based session is very likely the actual long-term design for this app regardless of how the frontend evolves, building the `localStorage`/body-token version first and migrating to cookies later would mean touching the login endpoint, every frontend fetch call, and the WebSocket handshake auth twice for no benefit in between.

## Decision
The session token is delivered via a `Set-Cookie` response header on login, using Spring's `ResponseCookie` builder with `HttpOnly` and `SameSite=Lax` set, rather than being returned in the `LoginResponse` JSON body. The token is never exposed to frontend JavaScript.

This has three follow-on requirements, all treated as part of this same decision rather than separate ones:
- `WebConfig`'s CORS mapping must set `.allowCredentials(true)`, which browsers require before they'll attach a cookie to a cross-origin request at all. This is only valid alongside an exact-origin `allowedOrigins` value (already the case), since browsers reject `allowCredentials(true)` combined with a wildcard origin.
- Every frontend `fetch()` call to the backend must explicitly pass `credentials: 'include'` — browsers do not attach cookies cross-origin by default even when one is held, regardless of `HttpOnly`.
- `TokenHandshakeInterceptor`, which currently reads the token from a `?token=...` query parameter on the WebSocket handshake URL, must instead read it from the `Cookie` header of the handshake's HTTP upgrade request. The query-parameter approach only worked because JS could read the token out of the login response body and build the URL itself; once the token is `HttpOnly`, JS can no longer see the value to put it there. Browsers attach cookies automatically to the handshake request regardless of `HttpOnly`, so reading it server-side from the `Cookie` header is a direct substitute.

## Consequences
Session tokens are no longer readable by any JavaScript running on the page, closing off an entire class of token-theft vector (XSS-driven exfiltration via `localStorage` or in-memory reads) that neither of the other two options addressed. This is a strictly stronger security posture than the body-token design it replaces, at essentially no cost to functionality — the browser now handles attaching the token on every request automatically, which also simplifies frontend code that would otherwise have to thread the token through manually.

Cross-origin cookie behavior is easy to misconfigure silently: forgetting `credentials: 'include'` on a new fetch call, or `allowCredentials(true)` on the backend, fails quietly (the request goes through but without the cookie, typically surfacing as an unexplained 401) rather than with an obvious error — this is a real footgun for any future endpoint added to either side.

This decision covers session token *transport* only, not lifetime. `SessionTokenStore` remains an in-memory map with no expiry, and a cookie's `Max-Age` only bounds how long the browser retains it, not whether the server still honors it after that point. Token expiry (and, relatedly, logout — explicit invalidation) remains a fully open, separate decision, deliberately not bundled into this one.
