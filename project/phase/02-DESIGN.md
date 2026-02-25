# Phase 2 – Design

## Ticket Status

| ID | Title | Status |
|---|---|---|
| – | – | – |

> Tickets are created during PM mode at the start of this phase. Update this table as work progresses.

## Objective

Design a single-screen layout where every interactive element maps to a haptic API discovered in Phase 1. The design must be minimal, black and white, and techy in feel.

## Design Direction

– **Black and white only, grey is also ok.** No color, no gradients. Monochrome palette — black background, white text/elements, or inverted. Shades of grey for hierarchy if needed.
– **Techy / lab instrument aesthetic.** Think oscilloscope, terminal, signal analyzer. Not playful, not Material.
– **No Material Design components.** No FABs, no bottom sheets, no snackbars, no Material cards. 
– **Single screen.** Everything visible or scrollable on one surface. No tabs, no drawers, no navigation.
– Simple and esthetic, mono font 

## Tasks

– Decide on the screen structure: how API categories from Phase 1 are laid out (vertical scroll with sections, grid, or other).
– Define what each interactive element looks like — button, touchable area, slider, etc. — for different types of haptic triggers.
– Design the visual feedback system — when a haptic fires, what does the user see? Options include: waveform animation, flash/pulse, ripple, text readout, or a combination.
– Ensure the layout handles unsupported effects gracefully (based on the decision from Phase 1).
– Produce a structured ASCII layout with annotations describing the screen layout. This is the expected wireframe format — no image tools are available.

## Deliverables

– **Screen layout**: Structured ASCII layout with annotations — what goes where, how it scrolls, how categories are separated.
– **Visual feedback strategy**: How the app visually confirms a haptic event (animation type, duration, placement).
– **Element design**: What each type of interactive trigger looks like and how the user interacts with it.

## Key Constraints

– The layout is driven by the API catalog from Phase 1. The number and grouping of elements comes from that research, not from design preference.
– Custom views that handle visual feedback (e.g., a waveform view, a pulse indicator) should be designed as extractable components — separate classes created in code, not XML.

## Questions to Resolve (present to user)

– What visual feedback style best fits the lab/instrument aesthetic — waveform animation, flash/pulse, numeric readout, or a combination?
– Should the screen scroll vertically as one long list of sections, or use a different layout strategy (e.g., collapsible sections, grid)?
– How should element sizing work — fixed sizes, or adaptive based on content density?
– Should there be a header/title area, or should the screen start directly with interactive elements?
