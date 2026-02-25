# 04 – Harder vertical drag resistance + softer spring

**Problem**:
1. Draggable item moves too easily up and down — should be much harder to move vertically.
2. Spring snap-back is too aggressive — reduce strength by ~30%.

---

## Ticket 1: UX-UI

**Status**: Done

- `verticalResistance`: 0.15 → 0.04 (nearly 4x harder, handle barely budges vertically)
- `springStiffness`: 800 → 560 (30% softer return)
- `springDamping`: 30 → 25 (proportionally reduced to maintain smooth damping ratio)

---

## Ticket 2: Development

**Status**: Done

Updated all three constants in DragThresholdView.

---

## Ticket 3: Review

**Status**: Done

- Vertical movement now barely responds (0.04 multiplier on dy)
- Spring return is noticeably gentler but still snaps back cleanly
- Damping ratio remains smooth (no oscillation)
- Build clean.
