# 06 – Double click dispatches two waves

**Problem**: The "Double Click" predefined effect button only spawns one wave, but it fires two haptic clicks. Should visually show two waves to match the haptic.

---

## Ticket 1: UX-UI

**Status**: Pending

Review how the double click effect works. The predefined effect `EFFECT_DOUBLE_CLICK` fires two clicks with a short delay (~100ms). The wave overlay should spawn two waves with matching timing — first wave immediately, second wave after the delay. Determine the right delay between waves.

---

## Ticket 2: Development

**Status**: Pending

In TremorActivity's `buildPredefinedEffectsSection`, detect when the effect is `EFFECT_DOUBLE_CLICK` and spawn a second wave after a short delay (use `postDelayed`). Both waves should originate from the same touch point.

---

## Ticket 3: Review

**Status**: Pending

Verify double click button shows two distinct expanding wave rings with visible timing offset. Other effect buttons still show single waves. No crashes or leaked runnables.
