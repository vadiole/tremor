# 05 – DrumRollerView inertia + button touch feedback

**Problem**:
1. DrumRollerView (the primitive intensity slider) has no inertia — scroll stops immediately on finger lift.
2. Plus/minus buttons on HapticCounter have no visual touch feedback (no color change on press).

---

## Ticket 1: UX-UI

**Status**: Pending

Review DrumRollerView scroll behavior. The ScrollWheelView already has fling/inertia — same pattern should apply here. For HapticCounter buttons, determine the right press visual: surface_pressed color on touch down, normal on release.

---

## Ticket 2: Development

**Status**: Pending

- Add fling/inertia to DrumRollerView: track velocity during drag, apply friction-based fling on release (same pattern as ScrollWheelView). Continue firing haptic ticks during fling. Clamp scale to 0-1 range.
- Add pressed color state to HapticCounter plus/minus buttons: change fill to surface_pressed on ACTION_DOWN, back to surface on ACTION_UP/CANCEL.

---

## Ticket 3: Review

**Status**: Pending

Verify DrumRollerView fling works smoothly, respects 0-1 bounds, fires haptics during inertia. Verify counter buttons show pressed state. No state bugs on rapid tapping.
