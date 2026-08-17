# 0023. Weighted-pool reward mode for non-currency actions

## Status
Accepted

## Context
[0011](0011-generalized-resource-and-primary-reward-model.md) established that every action has exactly one primary reward: a guaranteed, per-tick-scaled resource yield read from `action_primary_reward`. [0010](0010-action-and-rare-drop-model.md)/[0014](0014-rare-drop-rolling-algorithm.md) added `action_rare_drop` for bonus items dropped independently of the primary reward — each row is an independent Bernoulli trial per tick, so any number of distinct items can drop from the same tick, or none at all.

River Fishing doesn't fit either shape. It has no resource/currency reward at all — `applyPrimaryReward` currently treats the primary-reward row as mandatory, throwing 404 if one is absent — and its catch isn't several independent low-probability bonuses, it's a single, guaranteed catch per tick drawn from a mutually exclusive 9-item weighted pool (percentages summing to exactly 100%: four common fish at 20% each, two uncommon at 5.5% each, three rare at 3% each). Rolling it through `rollRareDrops` as nine independent Bernoulli trials would be both wrong (could yield zero, one, or several fish in the same tick instead of always exactly one) and wasteful (nine trials per tick for what should be a single weighted draw).

Two alternatives were considered:
- Branch on the action's identity (e.g. `action.getSkillKey().equals("fishing")`) directly inside `calculateOfflineProgress`, keeping today's two reward tables and adding an if/else around them. Rejected: every other part of the reward path (`applyPrimaryReward`, `rollRareDrops`) is generic and driven entirely by whatever rows exist for the action's key, with zero branching on action or skill identity anywhere in `PlayerCharacterService`. A name-based branch would be the first exception to that, and would need a new branch for every future action needing the same shape (a boss loot table, another gathering skill), rather than just new data.
- A wholly new table for weighted-pool entries, mirroring `action_rare_drop`'s shape but with a `weight` column instead of `drop_chance`. Rejected as unnecessary duplication: `action_rare_drop`'s existing `(action_key, item_key, drop_chance)` shape already stores exactly what a weighted pool needs — one numeric value per (action, item) pair. The meaning of that number (an independent probability vs. a relative weight) is a property of how the owning action rolls, not of the table itself.

## Decision
Add `reward_mode` to `game.action` (`VARCHAR(20) NOT NULL DEFAULT 'STANDARD' CHECK (reward_mode IN ('STANDARD', 'WEIGHTED_POOL'))`), backfilling all existing actions to `STANDARD`. `Action` gains a matching `RewardMode` enum field.

`calculateOfflineProgress` dispatches on `action.getRewardMode()`. Skill XP gain (`action.baseXp * n`) is unconditional and happens the same way for both modes — only the resource/item side differs:
- `STANDARD` actions keep calling `applyPrimaryReward` (mandatory resource yield) and `rollRareDrops` (independent Bernoulli bonus items), unchanged.
- `WEIGHTED_POOL` actions skip `applyPrimaryReward` entirely — no resource is credited — and instead call a new `rollWeightedPool(character, action, n)`, which performs one weighted draw per tick across the action's `action_rare_drop` rows (reinterpreted as selection weights rather than independent chances) and credits exactly one item per tick into `character_inventory`, using the same find-or-create semantics as `rollRareDrops`.

River Fishing (`river_fishing`, skill `fishing`) is seeded as the first `WEIGHTED_POOL` action, with its 9 fish seeded into `game.item` and their percentages (as decimals: `0.2000`, `0.0550`, `0.0300`, etc.) seeded into `action_rare_drop` as weights. It gets no `action_primary_reward` row, since none is read for `WEIGHTED_POOL` actions.

## Consequences
Adding a future weighted-pool action (another fishing spot, a mutually-exclusive boss-loot table) requires only data — a `reward_mode = 'WEIGHTED_POOL'` action row and its `action_rare_drop` weight rows, no `action_primary_reward` row — matching the zero-code-change extensibility already established for resources ([0011](0011-generalized-resource-and-primary-reward-model.md)) and rare drops ([0014](0014-rare-drop-rolling-algorithm.md)).

`action_rare_drop` now carries a dual meaning depending on its owning action's `reward_mode` (independent Bernoulli chance vs. mutually-exclusive weight) rather than a single fixed one. This is judged acceptable since the column's shape — a bare numeric value per action/item pair — is identical either way, and the correct interpretation is always resolvable by reading the one `reward_mode` value on the owning action. The trade-off is that a stray `action_rare_drop` row can no longer be interpreted in isolation, only in light of its action.

`applyPrimaryReward`'s "primary reward row is mandatory" assumption (404 if absent) now holds only for `STANDARD` actions; `WEIGHTED_POOL` actions are expected to have no `action_primary_reward` row at all, the opposite of every action seeded so far.

For a `STANDARD` action, each `action_rare_drop.drop_chance` is a true, independent probability by construction — it's compared directly against a random draw every tick, so it inherently has to be a value in `[0, 1]`. For a `WEIGHTED_POOL` action, `rollWeightedPool` normalizes by the pool's total weight (`weight_i / totalWeight`), so the raw stored values only need to be proportionally correct relative to each other; they aren't required to sum to `1`. River Fishing's weights were deliberately chosen to sum to exactly `1.0`, so each raw value happens to equal its actual selection probability and reads as a literal percentage — this is kept as a data-authoring convention for readability when seeding future `WEIGHTED_POOL` actions, not something the schema or algorithm enforces. A `WEIGHTED_POOL` pool that drifts from summing to `1` (e.g. a new item added without rebalancing the rest) remains functionally correct, just no longer literally readable as percentages.
