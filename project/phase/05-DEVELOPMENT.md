# Phase 5 – Development

## Ticket Status

| ID | Title | Status |
|---|---|---|
| DEV-01 | Manifest, Colors & Build Config | Done |
| DEV-02 | HapticEngine | Pending |
| DEV-03 | FlowLayout | Pending |
| DEV-04 | HapticButton | Pending |
| DEV-05 | DrumRollerView | Pending |
| DEV-06 | PrimitiveRow | Pending |
| DEV-07 | PatternBuilderView | Pending |
| DEV-08 | WaveOverlayView | Pending |
| DEV-09 | TremorActivity — Assembly & Wiring | Pending |
| DEV-10 | Edge Cases & Polish | Pending |

> Tickets are created during PM mode at the start of this phase. Update this table as work progresses.

## Objective

Build the app. Each ticket in this phase is a self-contained implementation task to be executed in developer role.

## Existing Scaffold

The project scaffold already exists and should be built upon, not recreated:
- Gradle project with build config at `app/build.gradle.kts`
- `TremorActivity.kt` at `app/src/main/kotlin/vadiole/tremor/` — basic Activity with programmatic FrameLayout and TextView
- Colors defined (`R.color.background`, `R.color.foreground`)
- Package: `vadiole.tremor`

Phase 5 tickets should extend this existing code, not start from scratch.

## Build Order

1. ~~**Project scaffold**~~ — Already exists (see above). Skip this step.
2. **Haptic engine** (if decided in Phase 4) — the wrapper class that exposes all haptic APIs.
3. **Layout** — the single-screen view hierarchy built in code with all sections and interactive elements as defined in Phase 2.
4. **Wiring** — Activity code that connects each UI element to its corresponding haptic API call.
5. **Visual feedback** — custom views or animations that fire when a haptic is triggered.
6. **Edge cases** — unsupported effect handling, no-vibrator fallback, system vibration disabled state.
7. **Polish** — final visual pass, spacing, consistency, any rough edges.

## Rules for Tickets in This Phase

– Every ticket must reference the specific requirement(s) it fulfills from `03-REQUIREMENTS.md`.
– Every ticket must reference the relevant section of `04-TECHNICAL-SPEC.md` for architecture guidance.
– Code output must be complete and functional — no stubs, no TODOs, no placeholders.
– If an implementation choice conflicts with the tech spec or design, flag it and switch back to PM mode — do not improvise.

## Notes

– No tests are part of this phase.
– The final output of this phase is a buildable, runnable Android project.
