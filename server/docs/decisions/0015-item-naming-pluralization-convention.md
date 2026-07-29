# 0015. Item naming pluralization convention

## Status
Accepted

## Context
Seeding the first two rows into `game.item` ([0012](0012-item-catalog-model.md)) in `V15` raised whether `item.name` should follow a uniform singular/plural rule. The existing resource names (`Ore`, `Hacksilver`) are singular/mass nouns, which would suggest singular as the established precedent — and that reads naturally for **Rune-Carved Bone**, a discrete craftable material, the same way nobody would say "Ores" or "Hacksilvers". **Grave Coins**, however, is currency-flavored, and "Coins" reads as a natural mass/collective noun in that context (comparable to "gold" or "coins" in other games) in a way "Grave Coin" does not — pluralizing it simply sounds better, with no gameplay or technical motivation behind the choice.

## Decision
There is no uniform singular/plural rule for `item.name`. Each item's display name is chosen for what reads best for that specific item, not for cross-row consistency: `Rune-Carved Bone` is singular, `Grave Coins` is plural. Quantity is always displayed separately from the name (e.g. a future `{quantity}x {name}` format), so nothing in the display layer or database ever needs to pluralize a name string — the choice of singular or plural in `item.name` is purely flavor text, never parsed or transformed by code.

## Consequences
Rows in `game.item` may look inconsistent side by side (some singular, some plural). This is intentional and should not be "corrected" into uniform pluralization by a future change without consulting this ADR. The tradeoff is explicitly in favor of each name reading well on its own over the catalog looking uniform as a whole; this has no schema or code impact, since `item.name` remains a plain, unparsed display string.
