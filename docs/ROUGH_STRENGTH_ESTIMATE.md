# Rough strength estimate

## Scope

`opponent-result-v1` produces a local, **low-confidence single-game performance signal** for
both White and Black only when a completed PGN provides both player ratings and a standard
result. It is not an official rating, a prediction of future playing strength, or a substitute
for a multi-game calibrated estimate.

## Formula

For a player with game score `s` (win = 1, draw = 0.5, loss = 0), the calculation adds one
virtual draw before inverting the standard Elo expected-score curve:

```text
regularizedScore = (s + 0.5) / 2
performance = opponentRating + 400 * log10(regularizedScore / (1 - regularizedScore))
```

The virtual draw avoids an infinite result for a single win or loss. A single game always has
low confidence, so the result includes a ±350-point range. The calculation deliberately does
not use centipawn loss or engine labels: mapping those signals to Elo requires a documented,
time-control-aware calibration dataset that is not yet available.

## Display rules

- Show it as a “rough game performance,” not a player rating.
- Always show the range, the opponent rating, “one rated game,” and “low confidence.”
- Do not show it for missing/invalid ratings, unfinished games, or unrecognized results.
- The final Game Summary UI remains pending the approved Figma frame reconciliation.
