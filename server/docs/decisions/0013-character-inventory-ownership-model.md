# 0013. Character inventory ownership model

## Status
Accepted (closes the per-character item-ownership placeholder left open by [0012](0012-item-catalog-model.md))

## Context
[0012](0012-item-catalog-model.md) added `game.item` as a catalog of items but deliberately stopped there, leaving per-character item ownership as a separate follow-up — tentatively named `character_inventory` rather than `character_item` to signal, per [0011](0011-generalized-resource-and-primary-reward-model.md)'s reasoning, that this is a genuine inventory concept rather than another `character_<lookup>` balance table like `character_resource`.

`character_resource` ([0011](0011-generalized-resource-and-primary-reward-model.md)) established a pattern for per-character balances: a composite-key row per `(character, resource)`, with a row pre-seeded for every resource at character creation — including zero-balance rows — because the set of resources is small and fixed, and every character conceptually "has" a balance (even if zero) in all of them from the moment they're created.

Items don't share that property. The item catalog is meant to grow open-endedly as new actions and rare drops are added, and a character owning zero of a given item is the overwhelming common case, not a meaningful state worth persisting. Pre-seeding a `character_inventory` row for every `game.item` at character creation, mirroring `character_resource`, would mean every character accumulates an ever-growing set of all-zero rows as the catalog grows, almost all of which they'll never interact with.

## Decision
Add `game.character_inventory` with a composite primary key `(player_character_id, item_key)`, foreign keys to `game.player_character` and `game.item`, and `quantity INT NOT NULL CHECK (quantity > 0)` — deliberately with no `DEFAULT` and no pre-seeded rows at character creation, unlike `character_resource`. A row exists if and only if a character owns at least one of that item; the `CHECK` constraint enforces "row exists ⇒ quantity ≥ 1" at the schema level, making a zero-or-negative-quantity row impossible to persist.

The entity/repository (`CharacterInventory`, `CharacterInventoryId`, `CharacterInventoryRepository`) mirror `CharacterResource`/`CharacterResourceId`/`CharacterResourceRepository` structurally, using the same `@EmbeddedId` composite-key pattern [0008](0008-skill-and-xp-tracking-model.md) established for `character_skill`. One deliberate difference: `quantity` is typed `int`, not `long` — the column is `INT`, not `BIGINT`, since items are counted in discrete, bounded stacks rather than accumulated indefinitely like a currency balance.

Because rows aren't pre-seeded, crediting an item requires find-or-create semantics in the service layer rather than the `findById(...).orElseThrow(...)` pattern used everywhere `character_resource` is read: look up by the composite id, increment `quantity` if a row is found, otherwise insert a new row with `quantity` set to whatever was just granted (never `0`, since crediting is only ever triggered by an actual successful grant — see [0014](0014-rare-drop-rolling-algorithm.md)).

## Consequences
Adding a new item to circulation requires no per-character backfill or migration — a character's first `character_inventory` row for that item is created lazily the moment it's actually granted, keeping `createCharacter` untouched and avoiding an ever-growing per-character row footprint as the item catalog grows.

The trade-off is that no code path can assume a `character_inventory` row exists the way every current `character_resource` lookup safely does; absence of a row must be treated as canonical zero, and any code crediting an item must branch on find-vs-create rather than doing a single unconditional update. A bulk `findByIdPlayerCharacterId` query — mirroring the one already on `CharacterResourceRepository`, for listing everything a character owns — is deliberately not added yet, since nothing calls it until inventory contents are exposed through the API; that's left for a follow-up once that read path is needed.
