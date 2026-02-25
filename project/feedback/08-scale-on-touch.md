# 08 – Scale-down on touch for all clickable elements

**Problem**: The scale-down press effect on HapticButton looks good. Apply the same effect to every clickable element in the app for consistency.

---

## Ticket 1: UX-UI

**Status**: Pending

Audit all clickable/tappable elements and determine which ones should get the scale effect:
- HapticButton — already has it (0.97x down, overshoot release)
- PrimitiveRow — tappable, needs it
- LongPressButton — tappable, needs it (but only scale, since it has its own progress visual)
- HapticCounter — has plus/minus buttons, needs it on each button
- RiseFallButton — press/hold, needs it
- DragThresholdView — handle drag, probably skip (drag is a different gesture)
- ScrollWheelView — scroll gesture, probably skip
- KeyboardRowView — individual key presses, needs it per key
- HapticToggle / toggle row — tap, needs it on the row
- Footer link — tap, probably skip (text link, scale feels wrong)
- Banner — tap, needs it

For each, determine if 0.97x is right or if a different scale suits the element size.

---

## Ticket 2: Development

**Status**: Pending

Add scale-down animation to all elements identified in UX-UI ticket. Use `animate().scaleX().scaleY()` with OvershootInterpolator on release. Keep the implementation consistent across all elements.

---

## Ticket 3: Review

**Status**: Pending

Tap every interactive element and verify: scale-down is visible, overshoot release is smooth, no elements were missed, no double-animation conflicts. Test rapid tapping.
