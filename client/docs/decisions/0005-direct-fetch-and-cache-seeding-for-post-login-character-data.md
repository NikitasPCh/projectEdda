# 0005. Direct fetch and cache seeding for post-login character data

## Status
Accepted

## Context
A "welcome back" progress modal, shown right after a real credential-based login, needed a reliable snapshot of the `progress` delta returned by `GET /players/character`. The natural first approach was to lean on the `character` query (`useQuery`, keyed `['character', playerId]`) that already drives the dashboard — capture whatever it resolved to via a `useEffect` reacting to `isSuccess`, gated by a one-shot flag set on login.

This ran into two related problems, both rooted in how TanStack Query's cache behaves:

- Within the same browser tab, re-logging in as a player whose `['character', playerId]` key was already cached from an earlier login in that tab caused `useQuery` to serve the *stale* cached snapshot instantly (its default stale-while-revalidate behavior), before a background refetch caught up moments later. The one-shot capture grabbed whichever `isSuccess` transition it saw first — the stale one, not the fresh one.
- Separately, since a `playerId` change is what makes `useQuery` fire its own fetch, nothing prevented that fetch from also happening independently of any deliberate login-time fetch — meaning two requests for the same character could reach the backend in close succession. Combined with `calculateOfflineProgress` not originally being safe under concurrent access (see server ADR `0020`), this could produce two different, both individually well-formed responses for the same login.

Three approaches were weighed for getting a reliably fresh, one-shot result at login:
- Continue relying on `useQuery`'s own resolution via a capture flag — the original approach, vulnerable to stale-cache serving as above.
- `queryClient.fetchQuery` — forces a real network fetch regardless of cache staleness and automatically writes its result into the cache. Rejected as more machinery than needed, given a plain fetch function already existed in the component for this exact request shape.
- A plain, direct call to the already-existing `fetchCharacter()` function, with its result manually written into the query cache via `queryClient.setQueryData`. Chosen for reusing an existing, simple primitive rather than introducing `fetchQuery` as new API surface.

Getting the direct-fetch approach fully correct took two more refinements once real-world testing surfaced remaining gaps. Seeding the cache had to happen *before* updating `playerId` state — `playerId` changing is what causes `useQuery` to observe the new key and decide whether to fetch, so updating it first let `useQuery` fire its own request before the seed had landed. And even with correct ordering, the default `staleTime: 0` meant freshly-seeded data was still immediately eligible for a mount-triggered refetch, so a `staleTime` was added to give just-seeded data a window where `useQuery` treats it as fresh rather than instantly stale again.

## Decision
`loginMutation`'s `onSuccess` performs a direct `fetchCharacter()` call, writes its result into the query cache via `queryClient.setQueryData(['character', data.playerId], freshCharacter)`, and only then updates `playerId`, `welcomeProgress`, and `view` state, in that order. The `character` query additionally sets `staleTime: 5000`, so data seeded or fetched within the last five seconds isn't treated as immediately stale.

## Consequences
A real login always drives the "welcome back" modal from one authoritative, freshly-fetched response, immune to whatever was previously cached for that player in the current tab. The dashboard's own `character` query benefits from the same seeded data instead of needing its own separate fetch immediately after login, eliminating the redundant request under normal conditions — verified via the browser's Network tab showing exactly one `/players/character` request per login rather than two.

This is deliberately paired with the backend's locking fix (`0020`): this change removes the redundant request in the common case, while the backend lock is what guarantees correctness even if a duplicate request does occur regardless.
