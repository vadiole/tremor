# Phase 3 – Product Requirements

## Ticket Status

| ID | Title | Status |
|---|---|---|
| – | – | – |

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

## Questions to Resolve (present to user)

– How should the app handle the VIBRATE permission — request at runtime, or just declare in manifest and assume it's granted?
– Should the app detect and show a message when system haptic feedback is globally disabled in device settings?
