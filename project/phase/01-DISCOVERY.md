# Phase 1 – Discovery

## Ticket Status

| ID | Title | Status |
|---|---|---|
| – | – | – |

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

## Questions to Resolve (present to user)

– Should the app support custom vibration pattern creation (user defines timing/amplitude arrays), or only trigger predefined effects?
– Should the app show a "device capabilities" summary (e.g., "your device supports X of Y effects"), or just let unsupported items be visibly disabled?
– How should unsupported effects be handled — hidden entirely, or shown as greyed-out with an explanation?
