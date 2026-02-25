# 02 – Fix toggle animation

**Problem**: Toggle has glitchy animation. Needs to be implemented correctly.

---

## Ticket 1: UX-UI

**Status**: Pending

Review HapticToggle.kt animation implementation. Identify the source of the glitch — could be OvershootInterpolator causing snap-back, incorrect thumb position range, double-fire from row + toggle click listeners, or animation not cancelling properly on rapid taps. Research correct toggle animation approach (smooth ease-out or slight overshoot, proper state management).

---

## Ticket 2: Development

**Status**: Pending

Fix the toggle animation based on UX-UI findings. Ensure:
- Smooth animation with no visual glitches
- Rapid tapping doesn't break the state
- Row click and toggle click don't conflict
- Proper cancellation of in-flight animations

---

## Ticket 3: Review

**Status**: Pending

Test toggle by rapid tapping, tapping the row vs the toggle itself, and tapping during animation. Verify no visual glitches, state stays in sync, haptic fires exactly once per toggle.
