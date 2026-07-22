# 0010. Action and rare-drop reward model

## Status
Accepted

## Context
The skill/XP model ([0008](0008-skill-and-xp-tracking-model.md)) tracks progression per category (e.g., `fighting`, `mining`), but doesn't capture that a single category can contain multiple distinct performable actions — fighting a rat versus fighting a wolf, or mining copper versus mining iron — each with its own reward economy and its own set of possible bonus/rare drops. Modeling every distinct action as a row in `game.skill` would conflate "what a character levels up in" with "the specific thing a character is doing," and wouldn't cleanly support adding harder tiers of an existing category later. A separate table per skill category (`fighting_action`, `mining_action`, etc.) was also considered, but rejected: it hard-codes the current set of skill categories into the schema's shape, and it breaks a clean foreign-key relationship for "what action is a character currently performing" — a single column can't validly reference "one of several possible tables" without giving up an enforced foreign key in favor of an application-level type discriminator.

Separately, a single action needs to be able to produce more than one possible rare/bonus reward, each occurring independently with its own chance, and those chances need to be stored precisely rather than as an approximate floating-point value.

## Decision
Add `game.action`, listing every concrete performable action, keyed by a natural string key (`fight_rat`, `mine_copper`, etc.) following the same convention as `game.skill`. Each action has a `skill_key` foreign key denoting which skill it grants XP toward, a `base_xp` value, and a gold reward range (`reward_min`/`reward_max`).

Add `game.action_rare_drop`, listing the possible rare/bonus drops for each action, with a composite primary key of `(action_key, item_key)` — the same reasoning as `character_skill`'s composite key: no rare-drop row needs an identity of its own, and the composite key lets a single action have any number of possible drops while still preventing duplicate entries for the same action-item pair. `drop_chance` is stored as `NUMERIC(5,4)` (mapped to `BigDecimal` in the entity layer) rather than a floating-point type, so the exact probability value entered is the exact value stored, with no binary floating-point rounding.

`item_key` is a plain string column with no foreign key for now, since no items/inventory table exists yet — it's a placeholder to be properly linked once that system is designed.

## Consequences
Adding a new action — a harder enemy tier, a new gathering node — is an `INSERT` into `game.action` plus optional rows in `game.action_rare_drop`, with no schema migration required. A character's currently-selected action can be a single, real foreign key column (`current_action_key` on `PlayerCharacter`) pointing at `game.action`, rather than needing a type discriminator to resolve which of several parallel tables it points into. If some action types eventually need fields that others don't — combat difficulty for fighting actions, for instance, with no equivalent for gathering — that can be added later via an optional extension table referencing `game.action(key)`, without altering or migrating any existing action rows. The trade-off accepted now is that `item_key` has no referential integrity yet, so a mistyped item key wouldn't be caught until an items table exists to validate against.
