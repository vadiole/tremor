# 04 – Harder vertical drag resistance + softer spring

**Problem**:
1. Draggable item moves too easily up and down — should be much harder to move vertically.
2. Spring snap-back is too aggressive — reduce strength by ~30%.

---

## Ticket 1: UX-UI

**Status**: Pending

Review current DragThresholdView parameters:
- `verticalResistance = 0.15f` — this multiplier on vertical delta. Lower = harder. Should be reduced significantly (e.g., 0.05 or less).
- `springStiffness = 800f` and `springDamping = 30f` — reduce stiffness by ~30% for a softer return.

Determine best values for a tight vertical feel and gentler spring.

---

## Ticket 2: Development

**Status**: Pending

Update DragThresholdView:
- Reduce vertical resistance multiplier (much harder vertical movement)
- Reduce spring stiffness by ~30% for softer snap-back
- Adjust damping proportionally if needed to maintain smooth animation

---

## Ticket 3: Review

**Status**: Pending

Verify dragging feels tight vertically (barely moves), horizontal drag is unchanged, and spring return is noticeably softer but still snappy enough to feel good.
