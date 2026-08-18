# 0026. A dedicated, unauthenticated ActionController for the action catalog

## Status
Accepted

## Context
The frontend needed a way to discover which actions exist at all, to build an action-selection picker — nothing exposed the `game.action` catalog itself; `player_character.current_action_key`/`pending_action_key` only ever referenced an action by key, never listed the available set. Two questions came up in the process of adding it: does this warrant a whole new controller for what's essentially one simple read, and — once `ActionController` exists — shouldn't `POST /players/character/action` (already in `PlayerController`) move there too, since it's also about "actions"?

On the first question: the endpoint itself is trivial (`actionRepository.findAll()` mapped to a small DTO), but `PlayerController`'s entire shape (`/players/...`) signals "this is about a specific player." A global, player-agnostic catalog read doesn't fit that URL semantically, regardless of how little code it takes to implement.

On the second, more substantive question: `POST /players/character/action` mutates `player_character.current_action_key`/`pending_action_key` — a specific character's state. It takes an `actionKey` as part of its payload, but what it's actually writing to is the character, not the action catalog, the same way a `POST /cart/items` endpoint mutates a cart even though its body references a product by ID — nobody would argue that belongs in a `ProductController`. Following "group by whatever's referenced" to its conclusion would also mean `GET /players/character` (which already returns skills, resources, *and* items in one response) would need to live in three different controllers depending on which field you're looking at, which is clearly wrong — it stays in `PlayerController` because the character is unambiguously the actual subject.

A third distinction, checked directly against `SecurityWebConfig`, reinforced the split: session authentication in this codebase is an explicit allowlist (`addPathPatterns("/players/character", "/players/character/action")`), not a blanket default — a new endpoint is public unless deliberately added to that list. `GET /actions` needs no session at all, being global data identical for every player; `POST /players/character/action` requires one, via `@RequestAttribute UUID playerId`. Putting both in one controller would mean that controller's contract is "public catalog reads, except for one method that secretly requires authentication and player identity" — a confusing shape, not a tidy one.

## Decision
`ActionController` is a new, standalone controller at `/actions`, containing only `GET /actions`, mapping `actionRepository.findAll()` to `List<ActionResponse>` (`key`, `name`). It is deliberately left off `SecurityWebConfig`'s protected path list — no session required, matching its non-player-specific nature.

`POST /players/character/action` stays exactly where it is, in `PlayerController`, unchanged.

## Consequences
`ActionController` stays narrow and easy to reason about at a glance: purely a read, no session handling, no business logic — a clean contract that would get muddied by mixing in an authenticated, player-scoped mutation.

This sets a pattern for any future catalog-style read (a standalone listing endpoint for `game.skill`, `game.resource`, or `game.item`, should one ever be needed): its own small, unauthenticated controller matching the resource being listed, not bundled into whichever controller happens to reference that data from a player's perspective.
