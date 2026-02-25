# Phase 1 – Discovery

## Ticket Status

| ID | Title | Status |
|---|---|---|
| DISC-01 | Catalog Android Haptic APIs | Done |
| DISC-02 | Competitive Analysis | Done |
| DISC-03 | Propose Feature Set & Grouping | Done |

> Tickets are created during PM mode at the start of this phase. Update this table as work progresses.

## Objective

Map out every Android haptic API available, understand what can be tested, and let that catalog drive the scope of the app. Also review what exists in the market to avoid reinventing poorly.

## Tasks

– Catalog all Android haptic APIs across API levels: `Vibrator`, `VibrationEffect`, `VibrationEffect.Composition`, `HapticFeedbackConstants`, and any others.
– For each API/effect, document: name, minimum API level, whether it requires specific hardware, parameters it accepts, and expected behavior.
– Research existing haptics testing apps and open-source projects. Note what they cover and what they miss. (Supplementary — web search results for niche apps may be limited. Do not block other work on this.)
– Based on the API catalog, propose which effects/APIs should be interactive elements on the single screen.
– Group the APIs into logical categories (e.g., predefined effects, primitive compositions, custom patterns, view-level feedback).

## Deliverables

– **API catalog**: A structured list of every haptic API/effect, organized by category, with API level and hardware requirements.
– **Competitive summary** (best-effort): Brief notes on existing tools found via web search — what they do, what they lack. This is supplementary; do not block on it.
– **Proposed feature set**: Which APIs to expose in the MVP, grouped by category. This directly becomes the content of the single screen.

## Questions Resolved

- **Custom patterns**: Simple minimal custom pattern for haptic composition primitives only (not raw vibration waveforms).
- **Device capabilities**: Show small text at bottom listing unavailable features.
- **Unsupported effects**: Hide entirely — only show what works on this device.

---

## API Catalog (DISC-01 Output)

### Overview

Android provides haptic feedback through several distinct API surfaces, from the original `Vibrator` service (API 1) to modern rich haptic primitives (API 30+).

### 1. HapticFeedbackConstants (View-level feedback)

**Class:** `android.view.HapticFeedbackConstants`
**Invocation:** `view.performHapticFeedback(constant)`
**Permission:** None required

| Constant | Min API | Description |
|---|---|---|
| `LONG_PRESS` | 3 | Long press on an object |
| `VIRTUAL_KEY` | 3 | Press on a virtual on-screen key |
| `KEYBOARD_TAP` | 8 | Press a soft keyboard key |
| `CLOCK_TICK` | 21 | Hour/minute tick of a clock |
| `CONTEXT_CLICK` | 23 | Context click on an object |
| `KEYBOARD_PRESS` | 27 | Virtual/software keyboard key press (preferred name for KEYBOARD_TAP) |
| `KEYBOARD_RELEASE` | 27 | Release a virtual keyboard key |
| `VIRTUAL_KEY_RELEASE` | 27 | Release a virtual key |
| `TEXT_HANDLE_MOVE` | 27 | Selection/insertion handle move on text field |
| `GESTURE_START` | 30 | Started a gesture |
| `GESTURE_END` | 30 | Finished a gesture |
| `CONFIRM` | 30 | Confirmation/success of user interaction |
| `REJECT` | 30 | Rejection/failure of user interaction |
| `TOGGLE_ON` | 34 | Toggle switch/button to on |
| `TOGGLE_OFF` | 34 | Toggle switch/button to off |
| `GESTURE_THRESHOLD_ACTIVATE` | 34 | Swipe/drag passed action threshold |
| `GESTURE_THRESHOLD_DEACTIVATE` | 34 | Moved back past gesture threshold |
| `DRAG_START` | 34 | Started drag-and-drop |
| `SEGMENT_TICK` | 34 | Switching between discrete choices |
| `SEGMENT_FREQUENT_TICK` | 34 | Switching between many choices (lighter) |

**Notes:** No VIBRATE permission needed. System maps each constant to device-specific vibration. Actual feel varies by OEM.

### 2. VibrationEffect — Predefined Effects

**Class:** `android.os.VibrationEffect`
**Invocation:** `VibrationEffect.createPredefined(effectId)` → `vibrator.vibrate(effect)`
**Min API:** 29 | **Permission:** VIBRATE

| Constant | Min API | Description |
|---|---|---|
| `EFFECT_CLICK` | 29 | Short, crisp click |
| `EFFECT_DOUBLE_CLICK` | 29 | Two rapid clicks |
| `EFFECT_TICK` | 29 | Light tick (lighter than click) |
| `EFFECT_HEAVY_CLICK` | 29 | Heavy click (stronger than click) |

**Support check:** `vibrator.areEffectsSupported(...)` returns YES/NO/UNKNOWN per effect (API 30+).

### 3. VibrationEffect.Composition — Haptic Primitives

**Class:** `android.os.VibrationEffect.Composition`
**Invocation:** `VibrationEffect.startComposition().addPrimitive(...).compose()` → `vibrator.vibrate(effect)`
**Min API:** 30 | **Permission:** VIBRATE

| Primitive | Min API | Description |
|---|---|---|
| `PRIMITIVE_CLICK` | 30 | Sharp, crisp click |
| `PRIMITIVE_QUICK_RISE` | 30 | Quick upward movement |
| `PRIMITIVE_SLOW_RISE` | 30 | Slow upward movement |
| `PRIMITIVE_QUICK_FALL` | 30 | Quick downward movement |
| `PRIMITIVE_TICK` | 30 | Very short light crisp tick |
| `PRIMITIVE_THUD` | 31 | Downward movement with reverberation |
| `PRIMITIVE_SPIN` | 31 | Spinning momentum |
| `PRIMITIVE_LOW_TICK` | 31 | Low-frequency light crisp tick |

**Parameters:** `addPrimitive(primitiveId, scale: Float [0.0–1.0], delay: Int [ms])`
**Support check:** `vibrator.arePrimitivesSupported(...)` returns boolean per primitive (API 30+).
**Hardware:** Requires linear resonant actuator (LRA); ERM motors generally don't support primitives.

### 4. VibrationEffect — One-Shot & Waveform

**Min API:** 26 | **Permission:** VIBRATE

| Method | Description |
|---|---|
| `createOneShot(ms, amplitude)` | Single vibration; amplitude 1–255 or DEFAULT_AMPLITUDE (-1) |
| `createWaveform(timings, repeat)` | Alternating off/on pattern |
| `createWaveform(timings, amplitudes, repeat)` | Pattern with per-step amplitude control |

**Note:** Requires `hasAmplitudeControl()` for smooth amplitude variation.

### 5. Vibrator Service & Capabilities

**Obtain:**
- API 1–30: `context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator`
- API 31+: `(context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator`

| Method | Min API | Returns | Description |
|---|---|---|---|
| `hasVibrator()` | 11 | Boolean | Device has a vibrator |
| `hasAmplitudeControl()` | 26 | Boolean | Supports smooth amplitude variation |
| `areEffectsSupported(...)` | 30 | IntArray | Per-effect support (YES=1, NO=2, UNKNOWN=0) |
| `arePrimitivesSupported(...)` | 30 | BooleanArray | Per-primitive support |

### 6. VibrationAttributes (API 33+)

Used to categorize vibrations by usage type. The app should use `USAGE_TOUCH` for all haptic triggers.

### 7. Permission

```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

Required for all `Vibrator.vibrate()` calls. Not required for `View.performHapticFeedback()`.

### API Surface Summary

| Category | API | Min API | Count | Permission |
|---|---|---|---|---|
| HapticFeedbackConstants | `View.performHapticFeedback()` | 3 | 20 constants | None |
| Predefined Effects | `VibrationEffect.createPredefined()` | 29 | 4 effects | VIBRATE |
| Composition Primitives | `VibrationEffect.Composition` | 30 | 8 primitives | VIBRATE |
| One-Shot | `VibrationEffect.createOneShot()` | 26 | duration + amplitude | VIBRATE |
| Waveform | `VibrationEffect.createWaveform()` | 26 | timings + amplitudes | VIBRATE |

---

## Competitive Analysis (DISC-02 Output)

### Play Store Apps

| App | Coverage | Strengths | Weaknesses |
|---|---|---|---|
| **Haptic Feedback Checker** (janeproject) | HapticFeedbackConstants only | Developer-focused, shows code snippets | Outdated (targets ~API 26), no VibrationEffect/Composition, no visual feedback |
| **Custom Vibrator / Haptic Test** (hannepps) | Custom waveform patterns only | Custom pattern creation & saving | No HapticFeedbackConstants, no predefined effects, no primitives, "dull sensations" |
| **Phone Vibration Tester** (myprorock) | Basic vibrator (amplitude slider, presets) | Clean diagnostic UI, amplitude slider, favorites | Motor diagnostic tool only, no haptic API coverage, no visual feedback |
| **Hapticlabs: Design Haptics** | Proprietary .HLA format | Professional haptic design, audio-haptic sync | Requires desktop companion app, not a standalone API tester |

### Open-Source Projects

| Project | Coverage | Strengths | Weaknesses |
|---|---|---|---|
| **AndroidHapticFeedbackTest** (GitHub, PBBB) | All 3 major surfaces (HapticFeedbackConstants, VibrationEffect, Composition) | Broadest coverage found, exposes hidden haptic IDs | Crashes on bad input, no visual feedback, no capability reporting, unpolished |
| **Google Platform Samples — Haptics** | Composition primitives only (4 demos) | Excellent visual-haptic pairing, authoritative | Only 4 primitives, educational sample not a tester, Compose-based |

### Key Gaps Across All Competitors

1. **No app covers all API surfaces** — HapticFeedbackConstants + Predefined Effects + Composition Primitives in one place
2. **No visual feedback** — haptics are invisible; no app clearly shows what fired
3. **No device capability reporting** — no app tells you what your device supports vs. doesn't
4. **No modern API 34 constants** — TOGGLE_ON/OFF, SEGMENT_TICK, DRAG_START etc. uncovered
5. **No clean single-screen UX** — all use tabs/navigation or are unpolished
6. **No parameter exploration** — no sliders for primitive scale/intensity
7. **No support-aware UI** — no app hides unsupported effects dynamically

### tremor Differentiation

- Only app covering all 5 API surfaces on one screen
- Visual feedback for every haptic trigger
- Device-aware: hide unsupported, note unavailable at bottom
- Minimal black-and-white lab aesthetic — unoccupied design space
- Primitive scale sliders for parameter exploration
- Modern API 34 constants included

---

## Proposed Feature Set & Grouping (DISC-03 Output)

### MVP Scope — What Appears on Screen

The single screen is a vertical scrollable list of sections. Each section is a category of haptic APIs. Within each section, each effect is a tappable element. Unsupported effects are hidden; a small text at the bottom lists what's unavailable.

### Section 1: Haptic Feedback

Source: `View.performHapticFeedback(constant)`
Permission: None
UI: Grid/list of tappable items, one per constant. Tap → feel + visual feedback.

Items (filtered at runtime by API level):

| Item Label | Constant | Min API |
|---|---|---|
| Confirm | `CONFIRM` | 30 |
| Reject | `REJECT` | 30 |
| Toggle On | `TOGGLE_ON` | 34 |
| Toggle Off | `TOGGLE_OFF` | 34 |
| Long Press | `LONG_PRESS` | 3 |
| Keyboard Press | `KEYBOARD_PRESS` | 27 |
| Keyboard Release | `KEYBOARD_RELEASE` | 27 |
| Clock Tick | `CLOCK_TICK` | 21 |
| Context Click | `CONTEXT_CLICK` | 23 |
| Gesture Start | `GESTURE_START` | 30 |
| Gesture End | `GESTURE_END` | 30 |
| Gesture Threshold Activate | `GESTURE_THRESHOLD_ACTIVATE` | 34 |
| Gesture Threshold Deactivate | `GESTURE_THRESHOLD_DEACTIVATE` | 34 |
| Text Handle Move | `TEXT_HANDLE_MOVE` | 27 |
| Virtual Key | `VIRTUAL_KEY` | 3 |
| Virtual Key Release | `VIRTUAL_KEY_RELEASE` | 27 |
| Drag Start | `DRAG_START` | 34 |
| Segment Tick | `SEGMENT_TICK` | 34 |
| Segment Frequent Tick | `SEGMENT_FREQUENT_TICK` | 34 |

Note: `KEYBOARD_TAP` excluded (same int value as `KEYBOARD_PRESS`). `NO_HAPTICS` excluded (produces nothing).

### Section 2: Predefined Effects

Source: `VibrationEffect.createPredefined(effectId)` → `vibrator.vibrate(effect)`
Permission: VIBRATE
Min API: 29
UI: Tappable items, one per effect. Tap → feel + visual feedback.

| Item Label | Constant | Min API |
|---|---|---|
| Click | `EFFECT_CLICK` | 29 |
| Double Click | `EFFECT_DOUBLE_CLICK` | 29 |
| Tick | `EFFECT_TICK` | 29 |
| Heavy Click | `EFFECT_HEAVY_CLICK` | 29 |

Runtime filter: check `vibrator.areEffectsSupported()` — hide effects returning NO.

### Section 3: Composition Primitives

Source: `VibrationEffect.startComposition().addPrimitive(id, scale).compose()` → `vibrator.vibrate(effect)`
Permission: VIBRATE
Min API: 30
UI: Each primitive is a tappable item. Additionally, each has a scale slider (0.0–1.0) to explore intensity.

| Item Label | Constant | Min API |
|---|---|---|
| Click | `PRIMITIVE_CLICK` | 30 |
| Tick | `PRIMITIVE_TICK` | 30 |
| Low Tick | `PRIMITIVE_LOW_TICK` | 31 |
| Quick Rise | `PRIMITIVE_QUICK_RISE` | 30 |
| Slow Rise | `PRIMITIVE_SLOW_RISE` | 30 |
| Quick Fall | `PRIMITIVE_QUICK_FALL` | 30 |
| Spin | `PRIMITIVE_SPIN` | 31 |
| Thud | `PRIMITIVE_THUD` | 31 |

Runtime filter: check `vibrator.arePrimitivesSupported()` — hide unsupported.

### Section 4: Composition Pattern Builder

Source: Same Composition API, but chaining multiple primitives.
Permission: VIBRATE
Min API: 30
UI: Simple builder — user picks 2–5 primitives from a list of supported ones, sets scale and delay for each, then plays the composed pattern. Minimal — not a full sequencer.

### Section 5: Device Info (Bottom)

Small text at the bottom of the screen:
- Device name / Android version / API level
- List of features not available on this device (hidden effects/primitives with their names)

### Excluded from MVP

| API | Reason |
|---|---|
| `VibrationEffect.createOneShot()` | Raw vibration, not haptic — out of scope per decisions |
| `VibrationEffect.createWaveform()` | Raw vibration patterns — out of scope per decisions |
| `VibratorManager` multi-vibrator | Niche, most devices have one vibrator |
| `VibrationAttributes` usage types | Internal implementation detail, not user-facing |
| Envelope effects (API 36) | Not yet stable |
