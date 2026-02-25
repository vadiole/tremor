# 28 – Performance & Code Review Tickets

## A. Reusable Patterns & Consistency

### A1. Extract shared press/release scale animation
**Status**: Done
**Files**: HapticButton, KeyButton, HapticCounter, LongPressButton, PrimitiveRow, RiseFallButton (6 views)
**Issue**: All 6 views duplicate the exact same animation code:
```kotlin
// Press:
animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
// Release:
animate().scaleX(1f).scaleY(1f).setDuration(150)
    .setInterpolator(OvershootInterpolator(2f)).start()
```
**Fix**: Created `View.animatePress()` / `View.animateRelease()` extension functions in `ViewExtensions.kt`. Cached shared `OvershootInterpolator(2f)`. Replaced all 6 usages.

### A2. Extract shared background + border drawing
**Status**: Skipped (too simple to warrant abstraction)

### A3. Deduplicate Vibrator acquisition and playPrimitive
**Status**: Done
**Fix**: Added `playPrimitive()` to `HapticEngine` with try-catch. RiseFallButton and HeartParticleView now accept a lambda instead of creating their own Vibrator.

### A4. Cache OvershootInterpolator instances
**Status**: Done (included in A1)

---

## B. Performance Issues

### B1. RiseFallButton: LinearGradient recreated every frame (HIGH)
**Status**: Done
**Fix**: Moved `LinearGradient` creation to `onSizeChanged()`. Only recreates when view size changes.

### B2. Path allocation in onDraw (MEDIUM)
**Status**: Done
**Fix**: Pre-allocated `Path` as field in HapticCounter and RiseFallButton. Use `path.reset()` before reuse. DragThresholdView was already fixed earlier.

### B3. WaveOverlayView: FloatArray allocations per frame (MEDIUM)
**Status**: Done
**Fix**: Pre-allocated `shaderOrigins`, `shaderRadii`, `shaderIntensities`, `shaderRingWidths` as fields.

### B4. HapticButton: TextUtils.ellipsize in onDraw (MEDIUM)
**Status**: Done
**Fix**: Cached `truncatedConstant` in `onMeasure`. Only recomputed when width changes.

### B5. PrimitiveRow: String.format in onDraw (LOW)
**Status**: Done
**Fix**: Cached `cachedValueText`, updated only in `drum.onValueChanged` callback.

### B6. ScrollWheelView/DrumRollerView: fragile velocity calculation (LOW)
**Status**: Done
**Fix**: Replaced `System.currentTimeMillis()` with `SystemClock.uptimeMillis()`. Added `maxVelocity` cap to prevent spikes.

### B7. RiseFallButton: clipRoundRect extension allocates Path (LOW)
**Status**: Done (combined with B2)
**Fix**: Removed `clipRoundRect` extension. Uses pre-allocated `clipPath` field directly.

### B8. FlowLayout: mutableListOf in onLayout (LOW)
**Status**: Done
**Fix**: Pre-allocated `rowHeights` IntArray as field. Grows only when needed.
