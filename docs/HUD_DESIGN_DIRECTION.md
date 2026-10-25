# Chessin' HUD design direction

**Owner direction recorded:** October 22, 2026

The owner selected the layout hierarchy from `stitch_chessin_logo_design(4).zip` as the working
review direction: game context, evaluation trajectory, board/replay controls, engine-grounded
coach explanation, then navigation. The generated cyan/magenta palette is explicitly *not* the
final product palette.

## Visual rules

- Use an original, readable HUD: restrained technical frames, corner brackets, data dividers, and
  sparse glow. Do not use the previous graffiti/splatter treatment in product navigation.
- Keep the board and review facts primary. Decorative lines must never reduce move, board, or
  evaluation legibility.
- Avoid invented telemetry such as sector names, UTC readings, radar degrees, session IDs, or
  military-console language when it is not real product data.
- Use plain product language: review, move, engine line, turning point, and coach explanation.
- All interaction assets are original vectors, designed on a 24 px grid. The first sheet is
  `docs/assets/hud/chessin-hud-icon-sheet.svg`; the matching launcher mark and command rail are
  `docs/assets/hud/chessin-hud-launcher-mark.svg` and `docs/assets/hud/chessin-hud-nav-rail.svg`.
  All three can be imported into Figma.

## Theme system

The application stores one of four color schemes in DataStore:

1. Arctic HUD — cool steel / ice blue.
2. Amber Operations — charcoal / amber.
3. Ultraviolet — black-violet / lavender.
4. Terminal Green — dark forest / phosphor green.

Theme selection controls backgrounds, panels, HUD frames, accent treatment, and board squares.
Critical review states retain their semantic meaning through dedicated success, warning, and
critical tokens; labels must remain visible alongside color.

## Screen decisions

- Keep Variant 4's information order and turning-point chart.
- Borrow the player summary and large focused board treatment from Variant 2.
- Borrow the compact selected navigation treatment from Variant 1.
- Borrow candidate-line comparison density from Variant 0 only after reducing tiny labels.
- Do not carry forward Variant 3's dense framing or fake tactical terminology.

This direction supersedes the earlier generated cyan/pink palette while the older Figma file is
offline. It is a working product direction, not a release approval; remaining screen-state,
accessibility, and final handoff reconciliation stay open.
