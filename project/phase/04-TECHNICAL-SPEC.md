# Phase 4 – Technical Specification

## Ticket Status

| ID | Title | Status |
|---|---|---|
| – | – | – |

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

## Questions to Resolve (present to user)

– Should there be a lightweight wrapper class for haptic calls (cleaner, but adds a file), or should the Activity handle everything directly (fewer files, but denser)?
