# 0009. Tracking offline progress

## Status
Accepted

## Context
A character's selected action produces results on a fixed interval (e.g. every 5 seconds) for as long as it's selected, and each result has variance — a randomized hacksilver amount within a range, and eventually a chance of rare drops. A character keeps "running" this action even while the player isn't connected, so progress needs to be reflected once they return.

Two broad approaches were considered for keeping this progress up to date. One is to eagerly tick every in-progress character on a fixed schedule — a background job that periodically applies one action's worth of results to every character currently performing one, whether or not anyone is looking at them. The other is to calculate progress lazily: store the point in time progress was last calculated for a character, and reconstruct elapsed progress from that timestamp on demand, whenever that character's state is next read or about to change.

## Decision
Calculate progress lazily rather than eagerly. `player_character` stores `last_calculated_at`, and whenever a character's state is read or is about to be mutated, elapsed wall-clock time since `last_calculated_at` is divided by the fixed action interval to get a whole number of completed actions, `n`. `n` actions' worth of XP and hacksilver are then applied at once, and `last_calculated_at` is advanced by exactly `n × interval` — not to "now" — so that leftover fractional time short of a full action carries forward to the next calculation instead of being silently discarded.

For the randomized hacksilver reward, rather than looping and drawing `n` individual samples — potentially a very large number of iterations for a character left offline for hours — the sum of `n` independent uniform draws is approximated as a single draw from a Normal distribution, using the summed mean and variance of those `n` draws (Central Limit Theorem). This is far cheaper for large `n`, at the cost of being a poor approximation at very small `n` (e.g. `n = 1`, where the true underlying distribution is uniform and strictly bounded, not bell-shaped); this is mitigated by clamping the sampled result to the range actually achievable, `[0, n × hacksilver_max]`.

## Consequences
No background scheduler is needed to keep characters progressing while players are offline — progress is reconstructed purely from elapsed time whenever it's next needed, keeping the system simple and avoiding spending compute on characters nobody is currently looking at. The trade-off is that a character's stored state is only ever correct as of the last time this calculation ran, so every code path that reads or mutates character state must remember to trigger it first, or progress will be silently stale — this is why it needs to run both when a character is read (e.g. fetching a character summary) and before a mutation that depends on current state (e.g. switching the selected action, which must settle progress under the *old* action first). The Normal-distribution approximation for hacksilver introduces a small statistical inaccuracy at low `n`, bounded by the range clamp; this should be revisited if a stricter per-action reward guarantee is ever needed.
