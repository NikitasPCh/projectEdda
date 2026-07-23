# 0011. Generalized resource tracking and primary action rewards

## Status
Accepted

## Context
`game.action`'s `hacksilver_min`/`hacksilver_max` columns and `player_character.hacksilver` (introduced when hacksilver was added as the game's currency, and consumed by the offline-progress calculation from [0009](0009-tracking-offline-progress.md)) assumed every action's scaled, randomized primary reward is currency. This breaks down once gathering-style actions are introduced — mining copper ore, for instance, has the same shape of reward (a per-action yield range, scaled by however many actions were performed) but isn't currency at all.

Modeling gathered resources through `game.action_rare_drop` ([0010](0010-action-and-rare-drop-model.md)) was considered and rejected: that table represents genuinely rare, probabilistic bonus drops, each with its own independent `drop_chance`. An action's primary yield isn't probabilistic in that sense — it happens every time the action completes, scaled by quantity, not gated by a chance of occurring at all. Forcing primary yield through that table would mean giving every gathering action a meaningless `drop_chance` of `1.0`, and giving rare drops a quantity range they don't otherwise need.

Modeling gathered resources as inventory items was also considered and rejected. Items imply concerns resources don't need — stacking limits, equip slots, and the rest of an eventual inventory system. Resources like copper ore are simpler than that: they're running per-character quantities, structurally identical to hacksilver, just not yet tracked as such.

## Decision
Introduce `game.resource`, a lookup table listing every trackable resource (including hacksilver itself), following the same reasoning as `game.skill`: a small, human-meaningful reference set keyed by a natural string key. Introduce `game.character_resource`, holding one row per (character, resource) pair with that character's quantity, using a composite primary key of `(player_character_id, resource_key)` — the same reasoning as `character_skill`: no row needs an identity of its own, and the composite key supports any number of resources per character without a schema change.

Introduce `game.action_primary_reward`, keyed by `action_key` (one row per action, a 1:1 relationship), specifying which `resource_key` an action's primary reward yields and its `yield_min`/`yield_max` range — replacing `action.hacksilver_min`/`hacksilver_max`. This mirrors `action_rare_drop`'s shape (an FK back to `game.action`, no FK yet on the resource/item side beyond what `game.resource` already provides), but models guaranteed, scaled yield rather than a probabilistic bonus.

Hacksilver stops being a special case: it becomes the first seeded row in `game.resource`, and every character's existing `hacksilver` balance is migrated into `character_resource` rows. `player_character.hacksilver` is dropped. This lets the offline-progress calculation apply the same mean/variance yield math (Central Limit Theorem approximation, as decided in [0009](0009-tracking-offline-progress.md)) regardless of which resource an action produces — it looks up the action's `action_primary_reward` row once, and updates `character_resource` generically, rather than needing one code path for hacksilver and another for every resource introduced afterward.

## Consequences
Adding a new resource — a new ore, a new fish — requires only `INSERT`s into `game.resource` and `game.action_primary_reward` for whichever actions yield it, no schema migration and no new calculation code, matching the extensibility already established for skills and rare drops. `action_rare_drop` itself is unaffected and continues to model genuinely rare, probabilistic bonus drops as a distinct concept from an action's guaranteed primary reward.

The trade-off is a real migration, not just new tables: `player_character.hacksilver` needs its data carried over into `character_resource` before the column is dropped, and `action.hacksilver_min`/`hacksilver_max` need the equivalent per-action data carried into `action_primary_reward` before those columns are dropped. `PlayerCharacterResponse`'s flat `hacksilver` field will need to become a list of resource balances, mirroring how skills are already returned — a breaking change for any existing consumer of that response. `PlayerCharacterService`'s offline-progress calculation needs rewriting to read `action_primary_reward`/write `character_resource` instead of the dedicated hacksilver fields it currently uses.
