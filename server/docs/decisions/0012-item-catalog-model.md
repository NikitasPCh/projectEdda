# 0012. Item catalog model

## Status
Accepted (closes the placeholder left by [0010](0010-action-and-rare-drop-model.md); per-character item ownership is a deliberately separate, not-yet-decided follow-up)

## Context
`game.action_rare_drop` ([0010](0010-action-and-rare-drop-model.md)) has always had a plain `item_key` string column with no foreign key, called out at the time as "a placeholder to be properly linked once that system is designed." Since then, [0011](0011-generalized-resource-and-primary-reward-model.md) modeled gathered resources (ore, and hacksilver itself) as generalized currency-like balances rather than items, reasoning that resources don't need item-specific concerns like stacking limits or equip slots — and explicitly left open whether genuine items would ever need their own model.

They do, now: items need a **rarity** classification (`common` through `unique`), a real attribute resources don't have and structurally shouldn't gain — folding rarity onto `game.resource` would mean every currency and gathered material carries a meaningless rarity value. This is the concrete trigger for giving items their own catalog rather than continuing to defer the decision.

Two shapes were considered for rarity itself: a plain string column on `game.item`, or a separate `game.item_rarity` lookup table (mirroring how `game.skill` and `game.resource` work). The lookup-table approach was rejected for now — rarity is a small, fixed set of values with no data of its own (no per-rarity color, weight, or drop-rate multiplier today), so a table would only exist to prevent typos. A `CHECK` constraint achieves the same typo protection without the extra table, at the cost of a migration (rather than a plain `INSERT`) if a new rarity tier is ever added.

## Decision
Add `game.item`, a catalog of every item that can exist in the game, keyed by a natural string key following the same convention as `game.skill`, `game.resource`, and `game.action`. Each item has a `name` and a `rarity` column constrained via `CHECK` to `('common', 'uncommon', 'rare', 'epic', 'legendary', 'unique')`.

Give `game.action_rare_drop.item_key` the foreign key to `game.item(key)` it was always missing, closing the specific gap `0010` flagged. There was no existing `action_rare_drop` data to migrate — the table has never had any seeded rows.

This step is deliberately scoped to the catalog only. It does not introduce a per-character item-ownership table (tentatively named `character_inventory` rather than `character_item`, to signal — per `0011`'s reasoning — that this is a genuine inventory concept, not another `character_<lookup>` balance table like `character_resource`), and it does not wire any rare-drop-rolling logic into `PlayerCharacterService`. Both are left for a follow-up decision once the ownership table's shape is settled.

## Consequences
Adding a new item — a rare drop for a new action, a future crafting material — is an `INSERT` into `game.item` plus a corresponding `game.action_rare_drop` row, with no schema migration required, matching the extensibility already established for skills, resources, and actions. `item_key` now has real referential integrity, so a mistyped item key in a future rare-drop seed will fail fast at migration time instead of silently existing as an orphaned reference.

The trade-off is that items currently have no home to live in once owned by a character — nothing can be dropped and credited to a character yet, since `character_inventory` doesn't exist. This ADR intentionally does not resolve that; it only unblocks the catalog side so item content and rare-drop definitions can start being modeled, while the ownership/inventory design is worked out separately. Adding a new rarity tier later requires an `ALTER TABLE` to widen the `CHECK` constraint rather than a plain `INSERT`, the accepted cost of not using a lookup table.
