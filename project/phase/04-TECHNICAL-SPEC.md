# Phase 4 – Technical Specification

## Ticket Status

| ID | Title | Status |
|---|---|---|
| SPEC-01 | Full Technical Specification | Done |

> Tickets are created during PM mode at the start of this phase. Update this table as work progresses.

## Objective

Define the minimal architecture, file structure, and API integration approach. Keep it as simple as the project demands — no over-engineering.

## Constraints Recap

– Kotlin, View-based UI built entirely in code. No XML layouts. No Compose.
– Single Activity. No Fragments unless absolutely necessary.
– 1–2 main files (Activity + optional helper/engine class).
– Custom views extracted into their own files where they represent reusable visual components (e.g., a waveform view, a pulse indicator).
– No persistence, no network, no settings, no dependency injection.

## Tasks

– Define the file/class structure: which classes exist, what each one does, where custom views live.
– Map each haptic API to how it gets triggered in code — a simple abstraction or just direct calls.
– Decide whether a thin haptic engine wrapper is worth it or if the Activity calls APIs directly.
– Specify the view hierarchy structure — how the single screen is composed programmatically (ScrollView, LinearLayout, etc., all in code).
– List any third-party dependencies (ideally zero).
– Define the Gradle/build configuration (min SDK, target SDK, permissions).

## Deliverables

– **File structure**: List of every file in the project with its purpose.
– **API integration plan**: How each haptic API category is called — direct calls vs. wrapper class.
– **Layout structure**: The programmatic view hierarchy of the single screen.
– **Build config**: Min SDK, target SDK, permissions, dependencies.

## Questions Resolved

- **Wrapper vs direct calls**: Use a lightweight `HapticEngine` object — keeps the Activity focused on UI, centralizes vibrator access and support checks. Single file, minimal abstraction.

---

## Technical Specification (SPEC-01 Output)

### Existing Project State

The project already has:
- `minSdk = 31`, `compileSdk = 36`, `targetSdk = 36`
- Edge-to-edge theme with transparent system bars
- Skeleton `TremorActivity` extending `Activity` (not AppCompat)
- `colors.xml` with background (#1B1B1B) and foreground (#FFFFFF)
- Single dependency: `androidx.core.ktx`

### Build Config Updates Needed

**AndroidManifest.xml** — add:
```xml
<uses-permission android:name="android.permission.VIBRATE" />
```
On the `<activity>` tag, add:
```xml
android:screenOrientation="portrait"
```

**colors.xml** — update background to pure black, add design tokens:
```xml
<color name="background">#000000</color>
<color name="foreground">#FFFFFF</color>
<color name="surface">#1A1A1A</color>
<color name="surface_pressed">#2A2A2A</color>
<color name="border">#333333</color>
<color name="text_secondary">#888888</color>
<color name="text_muted">#666666</color>
<color name="text_disabled">#555555</color>
```

### File Structure

```
app/src/main/kotlin/vadiole/tremor/
├── TremorActivity.kt          # Main Activity — builds view hierarchy, wires events
├── HapticEngine.kt            # Haptic API wrapper — vibrator access, support checks, trigger methods
├── view/
│   ├── WaveOverlayView.kt     # Full-screen wave animation overlay
│   ├── HapticButton.kt        # Tappable button for HapticFeedbackConstants & Predefined Effects
│   ├── PrimitiveRow.kt        # Primitive item with label, drum roller, scale readout
│   ├── DrumRollerView.kt      # Scrollable drum roller custom view (horizontal lines)
│   ├── FlowLayout.kt          # Wrapping flow layout for button grids
│   └── PatternBuilderView.kt  # Pattern composition area (add/remove/play)
```

**8 files total.** One Activity, one engine, six custom views.

### Class Responsibilities

#### TremorActivity.kt
- `onCreate()`: Build the full view hierarchy programmatically. Get `HapticEngine`. Populate sections based on support checks. Set up the wave overlay. Check haptic enabled state.
- `onResume()`: Re-check haptic enabled state, show/hide banner.
- `onPause()`: Cancel vibrations, clear wave animations.
- Owns the root `FrameLayout` containing the `ScrollView` + `WaveOverlayView`.
- Passes a `spawnWave: (x: Float, y: Float) -> Unit` callback down to interactive views.

#### HapticEngine.kt
A singleton-style helper (or plain class instantiated in Activity). Responsibilities:
- Hold reference to `Vibrator` (via `VibratorManager.defaultVibrator`)
- `isHapticEnabled(): Boolean` — check `Settings.System.HAPTIC_FEEDBACK_ENABLED`
- `getSupportedEffects(): List<Int>` — filter EFFECT_CLICK etc. via `areEffectsSupported()`
- `getSupportedPrimitives(): List<Int>` — filter via `arePrimitivesSupported()`
- `getAvailableHapticConstants(apiLevel: Int): List<HapticConstantInfo>` — filter by API level
- `playEffect(effectId: Int)` — `vibrator.vibrate(VibrationEffect.createPredefined(effectId))`
- `playPrimitive(primitiveId: Int, scale: Float)` — single primitive composition
- `playPattern(primitives: List<PatternEntry>)` — multi-primitive composition
- `cancel()` — cancel ongoing vibration
- Data classes: `HapticConstantInfo(name, constantName, value, minApi)`, `PatternEntry(primitiveId, scale, delayMs)`

#### WaveOverlayView.kt
- Extends `View`, `match_parent` in both dimensions.
- Not clickable / not focusable — touches pass through.
- Maintains a list of active `Wave(x, y, startTime)` objects.
- `spawnWave(x: Float, y: Float)` — adds a new wave, caps at 10.
- **Rendering: AGSL RuntimeShader** (API 33+, but minSdk 31 so needs fallback or minSdk bump consideration).
  - Preferred: AGSL `RuntimeShader` — a single shader receives all active wave data as uniforms (origins, radii, intensities) and computes per-pixel output. GPU-native additive blending, smooth gradients, excellent performance even with many concurrent waves.
  - Fallback (API 31–32): Canvas-based `RadialGradient` per wave with `BlendMode.SCREEN`.
- **Wave ring gradient profile** — soft Gaussian-like curve, NOT a hard 0→peak→0 triangle:
  - The ring cross-section follows a smooth bell curve (approximated by `smoothstep` in shader or multi-stop gradient on Canvas).
  - Profile: `0.0 → ease-in → peak → ease-out → 0.0` across the ring width.
  - In AGSL: `smoothstep(outerEdge, mid, dist) * smoothstep(innerEdge, mid, dist)` produces a soft band.
  - Ring width ~40dp for a wider, softer glow.
- **Wave lifecycle**: expand speed ~800dp/s, duration ~600ms. Overall intensity fades out smoothly over the wave's lifetime (soft ease-out, not linear).
- `onDraw()`: Set shader uniforms (wave count, per-wave x/y/radius/intensity), draw a full-screen rect with the shader paint. Call `postInvalidateOnAnimation()` if any waves active.
- AGSL shader sketch:
  ```glsl
  uniform float2 waves[10];    // xy origins
  uniform float radii[10];     // current radius per wave
  uniform float intensities[10]; // current intensity (fades over lifetime)
  uniform int waveCount;
  uniform float2 resolution;
  uniform float ringWidth;

  half4 main(float2 fragCoord) {
      float brightness = 0.0;
      for (int i = 0; i < waveCount; i++) {
          float dist = distance(fragCoord, waves[i]);
          float ring = smoothstep(radii[i] - ringWidth, radii[i], dist)
                     * smoothstep(radii[i] + ringWidth, radii[i], dist);
          brightness += ring * intensities[i];
      }
      brightness = clamp(brightness, 0.0, 1.0);
      return half4(brightness, brightness, brightness, brightness);
  }
  ```

#### HapticButton.kt
- Extends `View`. Custom-drawn: dark rect background, border, two lines of text (name + constant name).
- Constructor params: `label: String`, `constantName: String`, `onTrigger: (x: Float, y: Float) -> Unit`.
- `onTouchEvent()`: On ACTION_DOWN, call `onTrigger` with coordinates. Handle pressed state for visual feedback.
- Draws text centered: label in white 13sp mono, constant name below in grey 9sp mono.

#### PrimitiveRow.kt
- Extends `ViewGroup` (simple manual layout).
- Contains: label `TextView` (left), scale readout `TextView`, `DrumRollerView` (right).
- Constructor params: `label: String`, `constantName: String`, `onTrigger: (scale: Float, x: Float, y: Float) -> Unit`.
- Tap on row (excluding drum) triggers the primitive at current scale.
- Drum scroll updates scale and readout text.

#### DrumRollerView.kt
- Extends `View`. Custom-drawn.
- Displays horizontal lines scrolling vertically within a clipped window.
- Properties: `minValue: Float`, `maxValue: Float`, `step: Float`, `value: Float`.
- Touch handling: vertical drag moves the drum. Fling support optional.
- `onValueChanged: ((Float) -> Unit)?` callback.
- Draws ~5 visible horizontal lines, center brightest, edges fading. 1px white lines, 8dp vertical spacing. Subtle border.

#### FlowLayout.kt
- Extends `ViewGroup`. Measures children, wraps to next row when width exceeds available space.
- Even spacing: 8dp horizontal and vertical gaps.
- Children are equal-width, calculated to fit 3 per row minus gaps.

#### PatternBuilderView.kt
- Extends `LinearLayout` (vertical).
- Manages a list of `PatternEntry` items, each rendered as a row with: name, scale drum, delay drum, remove button.
- "+ ADD" button at bottom — shows a `PopupWindow` listing supported primitives.
- "▶ PLAY" button — calls `HapticEngine.playPattern()`.
- Max 5 entries. Add button hidden at limit. Play button disabled when empty.

### View Hierarchy

```
FrameLayout (root, match_parent)                          [TremorActivity]
├── ScrollView (match_parent, fillViewport=true)
│   └── LinearLayout (vertical, padding=16dp)
│       ├── TextView "HAPTIC FEEDBACK"                     [section label]
│       ├── FlowLayout                                     [HapticButton × N]
│       │   ├── HapticButton "Confirm" / "CONFIRM"
│       │   ├── HapticButton "Reject" / "REJECT"
│       │   └── ... (up to 19, filtered by API)
│       ├── Space (24dp)
│       ├── TextView "PREDEFINED EFFECTS"                  [section label]
│       ├── FlowLayout                                     [HapticButton × N]
│       │   ├── HapticButton "Click" / "EFFECT_CLICK"
│       │   └── ... (up to 4, filtered by support)
│       ├── Space (24dp)
│       ├── TextView "PRIMITIVES"                          [section label]
│       ├── LinearLayout (vertical)                        [PrimitiveRow × N]
│       │   ├── PrimitiveRow "Click" / "PRIMITIVE_CLICK"
│       │   └── ... (up to 8, filtered by support)
│       ├── Space (24dp)
│       ├── PatternBuilderView                             [section 4]
│       ├── Space (32dp)
│       └── TextView "Not available on this device: ..."   [device info]
├── TextView "Haptic feedback is disabled..."              [banner, GONE by default]
└── WaveOverlayView (match_parent, not clickable)          [wave overlay]
```

### API Integration Map

| UI Element | API Call | Permission |
|---|---|---|
| HapticButton (Section 1) | `view.performHapticFeedback(constant)` | None |
| HapticButton (Section 2) | `vibrator.vibrate(VibrationEffect.createPredefined(effectId))` | VIBRATE |
| PrimitiveRow tap | `vibrator.vibrate(VibrationEffect.startComposition().addPrimitive(id, scale).compose())` | VIBRATE |
| DrumRoller step | `view.performHapticFeedback(CLOCK_TICK)` | None |
| Pattern play | `vibrator.vibrate(VibrationEffect.startComposition().addPrimitive(...)...compose())` | VIBRATE |

### Threading

- All haptic calls on the main thread (they're fast, non-blocking).
- Wave animations on the main thread via `invalidate()` / `postInvalidateOnAnimation()`.
- No background threads needed.

### Dependencies

- `androidx.core.ktx` (already present) — only for extension functions, not for UI.
- No additional dependencies.
