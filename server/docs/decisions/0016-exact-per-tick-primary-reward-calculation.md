# 0016. Exact per-tick primary reward calculation

## Status
Accepted (supersedes the Normal-distribution approximation for primary reward from [0009](0009-tracking-offline-progress.md); resolves the divergence [0014](0014-rare-drop-rolling-algorithm.md) noted between it and rare-drop rolling)

## Context
[0009](0009-tracking-offline-progress.md) approximated a character's summed primary-reward yield over `n` ticks as a single draw from a Normal distribution (Central Limit Theorem), rather than looping `n` individual uniform draws, to avoid the cost of looping a potentially very large `n` for a character left offline for a long time. That approximation was accepted knowing it's a poor fit at small `n` — at `n = 1` the true distribution is a flat uniform range, not bell-shaped — mitigated only by clamping the sampled result to the achievable range.

[0014](0014-rare-drop-rolling-algorithm.md) already established that looping `n` exact trials is acceptable cost at realistic scale (its own worst-case estimate: tens of millions of cheap comparisons, sub-second, for six months offline at a five-second interval with ten rare-drop rows), specifically because the Normal approximation is unusable for rare, low-probability Bernoulli events. It left primary reward's Gaussian approximation in place at the time, noting the two mechanisms were "intentionally allowed to diverge."

That divergence stops being tenable once live, connected play is introduced: a planned feature will call this same calculation on a short, regular interval while a player is actively online, meaning `n = 1` becomes the common case rather than an occasional one (previously only hit if a player happened to check back within a single tick of going offline). The Normal approximation's known weakness at small `n` would then be visible on essentially every live update, not just an edge case bounded by a clamp.

## Decision
Replace the Normal-distribution approximation with an exact per-tick loop, structurally identical to `rollRareDrops`: for each of the `n` ticks, draw one uniform integer in `[yieldMin, yieldMax]` and sum. This is exact by construction, so the post-hoc clamp `0009` needed is no longer necessary — a sum of bounded draws is already bounded.

This is implemented as a private `applyPrimaryReward(character, action, n, coefficient)` helper on `PlayerCharacterService`, replacing the inline Gaussian block, and called once per `calculateOfflineProgress` invocation alongside the existing `rollRareDrops(character, action, n)`. The `coefficient` parameter (default `1.0`, the only value used today) scales both XP and per-tick yield. It is added now, ahead of need, in anticipation of a planned reward-bonus-period feature that will need to apply a multiplier to a subset of a resolved window's ticks — so the reward math itself won't need restructuring when that lands; only a separate "how many of these `n` ticks fall under which coefficient" concern will need to be built.

`calculateOfflineProgress`, `applyPrimaryReward`, and `rollRareDrops` all stop being `void` and now return what was actually credited during that call — skill and XP gained, resource and quantity gained, items gained (`ActionProgressResponse`) — instead of only mutating state silently. This is a prerequisite for a planned "welcome back" summary (showing a returning player what accrued while they were away) and for a planned live-play feature, though neither consumer is wired up by this change — callers (`getCharacterSummary`, `selectAction`) currently discard the returned value unchanged from their prior behavior.

## Consequences
Primary reward is now exact regardless of `n`, removing the small-`n` inaccuracy `0009` accepted and closing the divergence `0014` noted between it and rare-drop rolling — both mechanisms now use the same exact per-tick approach for the same underlying reason (independent per-tick draws, summed).

This adds an `O(n)` loop to primary-reward calculation where it was previously `O(1)` (a single Gaussian draw regardless of `n`). This is judged acceptable on the same grounds `0014` already used to accept `O(n)` for rare drops — it is no longer the only, or even the first, `O(n)` component of `calculateOfflineProgress`.

The `coefficient` parameter and the delta-returning shape are both ahead of any actual consumer: no caller passes a coefficient other than `1.0`, and no caller yet surfaces the returned `ActionProgressResponse` through the API. Wiring a welcome-back response into the player-facing endpoints, and building the tick-splitting logic a bonus-period feature would need to produce a non-default coefficient, are both explicitly deferred, separate follow-up work.
