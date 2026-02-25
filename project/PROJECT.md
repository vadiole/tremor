# tremor – Project Description & Plan

## What Is This

A single-screen Android app that lets users explore and test every available haptic feedback API on their device. The UI is driven by what APIs exist — discovery of available haptics comes first, and the interface is built around that.

## Goals

– Expose all available Android haptic APIs as interactive, testable elements on a single screen.
– Provide clear visual feedback for each haptic event (the UI must make the invisible visible).
– Be dead simple — one screen, no navigation, no settings, no onboarding.

## Constraints

– **Single screen app.** No navigation, no settings, no multi-screen flows.
– **Black and white, minimal, techy aesthetic.** No Material Design. No color. Think terminal / lab instrument.
– **Kotlin + View-based UI in code.** No XML layouts. No Jetpack Compose. All views created programmatically.
– **Minimal architecture.** 1–2 main files. Custom views in separate files where extraction makes sense.
– **No tests.** No unit tests, no instrumented tests, no QA phase.
– **No backend.** Fully self-contained APK.
– **API-driven UI.** The set of haptic APIs available on Android determines what appears on screen. Discovery comes before design.

## Target Audience

– Android developers exploring haptic capabilities.
– QA testers verifying haptic behavior.
– Curious users who want to feel what their phone can do.

## Success Criteria

– Every standard Android haptic API is represented as an interactive element.
– Visual feedback clearly accompanies every haptic trigger.
– The app runs on a single Activity with no navigation.
– The codebase fits in 1–2 main files plus extracted custom views.

---

## Phase Plan

| # | Phase | Purpose | Output | Status |
|---|---|---|---|---|
| 1 | Discovery | Research all Android haptic APIs, analyze competitors | API catalog, feature list, MVP scope | In progress |
| 2 | Design | Define the single-screen layout and interaction model | Screen layout, visual feedback strategy | Not started |
| 3 | Requirements | Formalize what the app must do | Product requirements document | Not started |
| 4 | Technical Spec | Define architecture, file structure, API mapping | Tech spec document | Not started |
| 5 | Development | Build the app | Working APK | Not started |

---

## Decisions Log

| Date | Phase | Decision | Rationale |
|---|---|---|---|
| 2026-02-25 | Discovery | Simple minimal custom pattern for haptic composition only (not raw vibration) | Keep scope tight, focus on HapticFeedback/Composition primitives |
| 2026-02-25 | Discovery | Show small text at bottom listing unavailable features | Inform user without cluttering UI |
| 2026-02-25 | Discovery | Hide unsupported effects entirely | Keep UI clean, only show what works on this device |
