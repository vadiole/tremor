# 28 – Performance & Code Review Tickets

## A. Reusable Patterns & Consistency

### A1. Extract shared press/release scale animation
**Status**: Pending
**Files**: HapticButton, KeyButton, HapticCounter, LongPressButton, PrimitiveRow, RiseFallButton (6 views)
**Issue**: All 6 views duplicate the exact same animation code:
```kotlin
// Press:
animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
// Release:
animate().scaleX(1f).scaleY(1f).setDuration(150)
    .setInterpolator(OvershootInterpolator(2f)).start()
```
**Fix**: Create `View.animatePress()` / `View.animateRelease()` extension functions in a shared file (e.g., `ViewExtensions.kt`). Cache a shared `OvershootInterpolator(2f)` constant. Replace all 6 usages.

### A2. Extract shared background + border drawing
**Status**: Pending
**Files**: HapticButton, KeyButton, HapticCounter, LongPressButton, PrimitiveRow, RiseFallButton, ScrollWheelView, DragThresholdView (8+ views)
**Issue**: Nearly every view declares the same `bgPaint`, `borderPaint`, `rect`, `cornerRadius`, `halfStroke` pattern and draws the same rounded rect bg + border. Colors, corner radius, and stroke width are identical across most views.
**Fix**: Create a helper function or base pattern (e.g., `drawBackground(canvas, rect, cornerRadius, bgPaint, borderPaint)`) or extract paint construction to shared factory. Consider whether a lightweight `CardView`-style base class makes sense, but avoid over-abstraction — extension functions are simpler.

### A3. Deduplicate Vibrator acquisition and playPrimitive
**Status**: Pending
**Files**: HapticEngine, RiseFallButton, HeartParticleView
**Issue**: `Vibrator` is independently obtained from `VibratorManager` in 3 different places. `playPrimitive()` is duplicated verbatim in `RiseFallButton` and `HeartParticleView`. Additionally, vibrations from these views are NOT cancelled when `hapticEngine.cancel()` is called in `onPause()`.
**Fix**: Pass the `HapticEngine` instance (or just the `Vibrator`) to views that need it. Alternatively, add a `playPrimitive()` method to `HapticEngine` and pass it as a lambda.

### A4. Cache OvershootInterpolator instances
**Status**: Pending
**Files**: HapticButton, KeyButton, HapticCounter, LongPressButton, PrimitiveRow, RiseFallButton
**Issue**: `OvershootInterpolator(2f)` is allocated on every ACTION_UP in 6 views (once per finger lift). While individual allocations are cheap, this is unnecessary object churn.
**Fix**: Define a shared companion constant `val OVERSHOOT = OvershootInterpolator(2f)` and reuse it. If doing A1, this is included automatically.

---

## B. Performance Issues

### B1. RiseFallButton: LinearGradient recreated every frame (HIGH)
**Status**: Pending
**File**: RiseFallButton.kt, line ~82
**Issue**: The condition `fillPaint.shader == null || width > 0` is always `true` when the view has a width, causing a new `LinearGradient` shader to be compiled on every single draw call during animation. This is expensive — GPU shader compilation per frame.
**Fix**: Create the `LinearGradient` once in `onSizeChanged()` and cache it. Only recreate when the view size actually changes.

### B2. Path allocation in onDraw (MEDIUM)
**Status**: Pending
**Files**: HapticCounter.kt (~line 79), DragThresholdView.kt (~line 155), RiseFallButton.kt (clipRoundRect extension)
**Issue**: `Path()` is allocated every frame in `onDraw` for round-rect clipping. This creates GC pressure during animation.
**Fix**: Pre-allocate `Path` as a field, call `path.reset()` before reuse. DragThresholdView already fixed in this iteration.

### B3. WaveOverlayView: FloatArray allocations per frame (MEDIUM)
**Status**: Pending
**File**: WaveOverlayView.kt, lines ~89-92
**Issue**: Four `FloatArray` allocations (`origins`, `radii`, `intensities`, `ringWidths`) happen every frame in `drawWithShader()` during wave animation. With 10 waves active, this is 4 arrays per frame at 60fps.
**Fix**: Pre-allocate these as fields and reuse them. The arrays are fixed-size (`maxWaves * 2`, `maxWaves`, etc.).

### B4. HapticButton: TextUtils.ellipsize in onDraw (MEDIUM)
**Status**: Pending
**File**: HapticButton.kt, line ~107
**Issue**: `TextUtils.ellipsize()` performs text measurement and potentially allocates a new `CharSequence` every frame. This runs every time the button redraws (during press/release animation).
**Fix**: Cache the ellipsized result in `onMeasure` or `onSizeChanged` (when width changes). Only recompute when width actually changes.

### B5. PrimitiveRow: String.format in onDraw (LOW)
**Status**: Pending
**File**: PrimitiveRow.kt, line ~100
**Issue**: `String.format("%.2f", drum.value)` allocates a new String every frame. Only fires during drum interaction, so impact is limited.
**Fix**: Cache the formatted string and only reformat when the value changes.

### B6. ScrollWheelView/DrumRollerView: fragile velocity calculation (LOW)
**Status**: Pending
**Files**: ScrollWheelView.kt, DrumRollerView.kt
**Issue**: Velocity is computed as `dx / dt * 16f` — this normalization to ~16ms is fragile. If a MOVE event arrives after 1ms, velocity spikes. Also uses `System.currentTimeMillis()` which can jump on clock changes.
**Fix**: Use `VelocityTracker` or `SystemClock.uptimeMillis()` for reliable velocity estimation. Cap velocity magnitude.

### B7. RiseFallButton: clipRoundRect extension allocates Path (LOW)
**Status**: Pending
**File**: RiseFallButton.kt, clipRoundRect extension
**Issue**: The `Canvas.clipRoundRect` extension creates a new `Path()` on every call from onDraw. Same issue as B2 but in an extension function.
**Fix**: Combine with B2 — pre-allocate the clip path as a field.

### B8. FlowLayout: mutableListOf in onLayout (LOW)
**Status**: Pending
**File**: FlowLayout.kt, line ~47
**Issue**: `mutableListOf<Int>()` for `rowHeights` is created on every layout pass. Minor allocation.
**Fix**: Pre-allocate an IntArray sized to `ceil(childCount / columns)` and reuse, or keep as-is since layout passes are infrequent.
