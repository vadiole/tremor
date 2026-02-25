# 02 – Fix toggle animation

**Problem**: Toggle has glitchy animation. Needs to be implemented correctly.

---

## Ticket 1: UX-UI

**Status**: Done

**Root cause**: `OvershootInterpolator(1.2f)` pushes `thumbPosition` beyond the 0-1 range (e.g., to ~1.1 or ~-0.1). The `blendColor()` function and thumb X calculation don't clamp, producing:
- Garbage color values (ARGB channels overflow/underflow)
- Thumb jumping outside the track bounds

**Fix**: Replace `OvershootInterpolator` with `DecelerateInterpolator` for a smooth ease-out. Toggles should feel crisp, not bouncy. Also clamp `fraction` in `blendColor()` for safety. Reduce duration to 200ms for snappier feel.

---

## Ticket 2: Development

**Status**: Done

- Replaced `OvershootInterpolator(1.2f)` with `DecelerateInterpolator(2f)` — no overshoot beyond 0-1 range.
- Reduced duration from 250ms to 200ms for snappier feel.
- Added `fraction.coerceIn(0f, 1f)` clamp in `blendColor()` as safety net.

---

## Ticket 3: Review

**Status**: Done

- `DecelerateInterpolator` output stays in 0-1 range — no color overflow possible.
- `thumbAnimator?.cancel()` properly cancels in-flight animation on rapid taps.
- Row click and toggle click don't conflict (child consumes click, parent only fires on non-toggle area).
- Build clean.
