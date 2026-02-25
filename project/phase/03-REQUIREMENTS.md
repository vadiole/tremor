# Phase 3 – Product Requirements

## Ticket Status

| ID | Title | Status |
|---|---|---|
| REQ-01 | Functional & Non-Functional Requirements | Done |
| REQ-02 | Edge Cases | Done |

> Tickets are created during PM mode at the start of this phase. Update this table as work progresses.

## Objective

Formalize what the app must do based on the API catalog (Phase 1) and the design (Phase 2). This document becomes the source of truth for what gets built.

## Tasks

– Translate the API catalog and design into a list of functional requirements (what the app does).
– Define non-functional requirements (minimum API level, performance expectations, permissions).
– Specify how each category of haptic effect is triggered, displayed, and handled when unsupported.
– Define edge cases: what happens when vibration is disabled system-wide, when the device has no vibrator, when the app loses focus during playback.

## Deliverables

– **Functional requirements list**: Every behavior the app must support, traceable back to a specific API or design decision.
– **Non-functional requirements list**: API level support, permission handling, performance.
– **Edge case handling**: Documented behavior for error states and unusual conditions.

## Notes

– Requirements should be specific enough that they can be implemented without ambiguity when switching to developer role.
– Each requirement should be verifiable — "the app does X when the user does Y" format.

## Questions Resolved

- **VIBRATE permission**: Manifest declaration only. No runtime request needed (normal permission).
- **System haptics disabled**: Pinned banner at bottom of screen. Tappable to open Settings. Non-blocking — content still usable. Disappears on resume if user fixed it.

---

## Functional Requirements (REQ-01 Output)

### FR-01: Haptic Feedback Section

- FR-01.1: The app displays a section labeled "HAPTIC FEEDBACK" containing one tappable button per `HapticFeedbackConstants` constant.
- FR-01.2: Each button is labeled with a human-readable name (e.g., "Confirm", "Reject", "Toggle On").
- FR-01.3: Tapping a button calls `view.performHapticFeedback(constant, FLAG_IGNORE_VIEW_SETTING)` and spawns a wave animation from the touch point.
- FR-01.4: Only constants supported on the device's API level are shown. Constants requiring a higher API level are hidden.
- FR-01.5: The following 19 constants are included (excluding `KEYBOARD_TAP` duplicate and `NO_HAPTICS`): LONG_PRESS, VIRTUAL_KEY, CLOCK_TICK, CONTEXT_CLICK, KEYBOARD_PRESS, KEYBOARD_RELEASE, VIRTUAL_KEY_RELEASE, TEXT_HANDLE_MOVE, GESTURE_START, GESTURE_END, CONFIRM, REJECT, TOGGLE_ON, TOGGLE_OFF, GESTURE_THRESHOLD_ACTIVATE, GESTURE_THRESHOLD_DEACTIVATE, DRAG_START, SEGMENT_TICK, SEGMENT_FREQUENT_TICK.

### FR-02: Predefined Effects Section

- FR-02.1: The app displays a section labeled "PREDEFINED EFFECTS" containing one tappable button per `VibrationEffect` predefined effect.
- FR-02.2: Tapping a button creates the effect via `VibrationEffect.createPredefined(effectId)` and calls `vibrator.vibrate(effect)`, then spawns a wave animation.
- FR-02.3: The section is only visible on API 29+.
- FR-02.4: Individual effects are hidden if `vibrator.areEffectsSupported()` returns `VIBRATION_EFFECT_SUPPORT_NO` (API 30+). On API 29, all 4 are shown (no support-checking API available).
- FR-02.5: The 4 effects are: EFFECT_CLICK, EFFECT_DOUBLE_CLICK, EFFECT_TICK, EFFECT_HEAVY_CLICK.

### FR-03: Composition Primitives Section

- FR-03.1: The app displays a section labeled "PRIMITIVES" containing one row per supported `VibrationEffect.Composition` primitive.
- FR-03.2: Each row shows: primitive name (left), numeric scale readout, drum roller control (right).
- FR-03.3: The drum roller adjusts the scale parameter from 0.0 to 1.0 in 0.05 increments. Default scale is 1.0.
- FR-03.4: Scrolling the drum fires a haptic tick feedback on each step change.
- FR-03.5: Tapping the row (outside the drum) creates a single-primitive composition at the current scale via `VibrationEffect.startComposition().addPrimitive(id, scale).compose()` and calls `vibrator.vibrate(effect)`, then spawns a wave animation.
- FR-03.6: The section is only visible on API 30+.
- FR-03.7: Individual primitives are hidden if `vibrator.arePrimitivesSupported()` returns false.
- FR-03.8: The 8 primitives are: PRIMITIVE_CLICK, PRIMITIVE_TICK, PRIMITIVE_LOW_TICK, PRIMITIVE_QUICK_RISE, PRIMITIVE_SLOW_RISE, PRIMITIVE_QUICK_FALL, PRIMITIVE_SPIN, PRIMITIVE_THUD.

### FR-04: Pattern Builder Section

- FR-04.1: The app displays a section labeled "PATTERN" with a composition builder area.
- FR-04.2: The section is only visible on API 30+ and only if at least one primitive is supported.
- FR-04.3: The user can add up to 5 primitives to the pattern via an "+ ADD" button.
- FR-04.4: Tapping "+ ADD" shows a popup listing all supported primitives by name. Tapping one adds it to the pattern.
- FR-04.5: Each added primitive row shows: name, scale drum (0.0–1.0), delay drum (0–500ms in 10ms steps), and a remove (✕) button.
- FR-04.6: Tapping "▶ PLAY" composes all primitives into a single `VibrationEffect` via `startComposition()` with each primitive's scale and delay, then calls `vibrator.vibrate(effect)` and spawns a wave animation.
- FR-04.7: The play button is disabled when the pattern is empty.
- FR-04.8: The "+ ADD" button is hidden when 5 primitives are already added.

### FR-05: Wave Animation

- FR-05.1: Every haptic trigger spawns a circular wave animation from the exact touch coordinates.
- FR-05.2: The wave is a translucent white gradient ring that expands outward at ~800dp/s and fades from ~0.3 alpha to 0 over ~600ms.
- FR-05.3: Multiple waves can coexist simultaneously with additive blending (overlapping regions appear brighter).
- FR-05.4: Maximum 10 concurrent waves. Oldest wave is removed when the limit is reached.
- FR-05.5: The wave overlay covers the entire screen and does not intercept touch events.

### FR-06: Device Info

- FR-06.1: At the bottom of the scroll content, small grey monospace text lists features not available on this device.
- FR-06.2: The list includes: hidden HapticFeedbackConstants (by API level), hidden predefined effects (by support check), hidden primitives (by support check), and entire hidden sections (by API level).
- FR-06.3: If all features are available, this text is hidden.

### FR-07: Haptics Disabled Banner

- FR-07.1: When system haptic feedback is disabled, a banner appears pinned at the bottom of the screen (above device info scroll content, fixed position).
- FR-07.2: The banner text reads: "Haptic feedback is disabled" with a tappable action to open the device's Sound & Vibration settings.
- FR-07.3: The banner is non-blocking — all content is still scrollable and interactive.
- FR-07.4: The app checks haptic status on every `onResume()`. If the user enables haptics in Settings and returns, the banner disappears.

---

## Non-Functional Requirements

### NFR-01: Target API

- NFR-01.1: `minSdkVersion` = 26 (Android 8.0). This is the minimum for `VibrationEffect`.
- NFR-01.2: `targetSdkVersion` = 35 (latest stable).
- NFR-01.3: The app gracefully degrades on API 26–29 by hiding sections that require higher APIs.

### NFR-02: Permissions

- NFR-02.1: The app declares `android.permission.VIBRATE` in the manifest.
- NFR-02.2: No runtime permission request is needed (VIBRATE is a normal permission).

### NFR-03: Performance

- NFR-03.1: Wave animations run at 60fps with hardware acceleration.
- NFR-03.2: Haptic trigger latency from touch to vibration is imperceptible (<16ms from touch event to `vibrate()` call).
- NFR-03.3: The scroll list renders smoothly with no jank even with all sections populated.

### NFR-04: Architecture

- NFR-04.1: Single Activity, no Fragments, no navigation.
- NFR-04.2: All views created programmatically in Kotlin — no XML layouts, no Jetpack Compose.
- NFR-04.3: Custom views (wave overlay, drum roller, flow layout) extracted into separate files.
- NFR-04.4: No external libraries.

---

## Edge Cases (REQ-02 Output)

| Condition | Detection | Behavior |
|---|---|---|
| Device has no vibrator | `vibrator.hasVibrator()` returns false on startup | Hide all sections that require VIBRATE permission (Predefined Effects, Primitives, Pattern). Only show HapticFeedbackConstants section (system handles those independently). Show "No vibrator detected" in device info text. |
| System haptic feedback disabled | Check `Settings.System.HAPTIC_FEEDBACK_ENABLED` on `onResume()` | Show pinned banner at bottom: "Haptic feedback is disabled. Tap to enable." Tapping opens `Settings.ACTION_SOUND_SETTINGS`. Banner disappears on next `onResume()` if setting is now enabled. Content remains usable. |
| No amplitude control | `vibrator.hasAmplitudeControl()` returns false | No direct impact — the app uses `VibrationEffect.createPredefined()` and `Composition` which don't require amplitude control. No special handling needed. |
| Predefined effect unsupported | `areEffectsSupported()` returns `SUPPORT_NO` for an effect | Hide that specific effect button. List it in device info text at bottom. |
| Predefined effect support unknown | `areEffectsSupported()` returns `SUPPORT_UNKNOWN` | Show the button — the effect may still work. If it fails silently at runtime, that's acceptable. |
| Primitive unsupported | `arePrimitivesSupported()` returns false for a primitive | Hide that primitive row. Hide it from the pattern builder's add popup. List it in device info text. |
| All primitives unsupported | `arePrimitivesSupported()` returns false for all 8 | Hide the entire Primitives section and the entire Pattern section. Note in device info. |
| All predefined effects unsupported | `areEffectsSupported()` returns NO for all 4 | Hide the entire Predefined Effects section. Note in device info. |
| Composition with 0 primitives | User taps play with empty pattern | Play button is disabled when pattern is empty. No action. |
| App loses focus during vibration | `onPause()` called | Call `vibrator.cancel()` to stop any ongoing vibration. Clear active wave animations. |
| Rapid repeated taps | User taps same button very quickly | Each tap triggers independently. The vibrator handles overlapping calls (new call cancels previous). Wave animations accumulate up to the 10-wave limit. |
| Drum scroll conflicts with page scroll | User scrolls vertically on a drum roller | Drum roller intercepts vertical touch events within its bounds (`requestDisallowInterceptTouchEvent`). Parent ScrollView does not scroll when touching the drum. |
| API 26–28 device | API level check | Only Haptic Feedback section is visible (HapticFeedbackConstants available since API 3). Predefined Effects (API 29+), Primitives (API 30+), and Pattern (API 30+) are all hidden. Device info explains what's unavailable. |
| Screen rotation | Configuration change | Lock to portrait orientation in manifest (`android:screenOrientation="portrait"`). No rotation handling needed. |
| Pattern builder: duplicate primitives | User adds same primitive twice | Allowed. Each entry is independent with its own scale and delay. |
