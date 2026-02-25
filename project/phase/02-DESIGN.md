# Phase 2 – Design

## Ticket Status

| ID | Title | Status |
|---|---|---|
| DES-01 | Screen Structure & Layout | Done |
| DES-02 | Element Design | Pending |
| DES-03 | Visual Feedback Strategy | Pending |

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

## Questions Resolved

- **Visual feedback**: Wave animation expanding from touch point — translucent gradient, real wave physics, overlapping on rapid taps/drags, possibly shader-based.
- **Scroll**: One long vertical list, no collapsible sections.
- **Header**: None — start directly with interactive elements.
- **Element sizing**: Flexible, but sliders are scrollable wheels with vertical scroll + power fill indicator.

---

## Screen Structure & Layout (DES-01 Output)

### Overall Structure

- Black background, full screen (no status bar tint, edge-to-edge)
- Single `ScrollView` containing a vertical `LinearLayout`
- No header, no title — content starts immediately
- Sections separated by subtle spacing (no dividers, no cards)
- Section labels: small monospace text, white, uppercase, left-aligned
- Wave animation overlay covers the entire screen (rendered on top of all content)
- Small device info text at the very bottom

### ASCII Wireframe

```
┌──────────────────────────────────┐
│░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│ ← wave overlay (full screen, transparent)
│                                  │   renders on top of everything
│  HAPTIC FEEDBACK                 │ ← section label (small mono, white)
│                                  │
│  ┌──────┐ ┌──────┐ ┌──────┐     │
│  │Confir│ │Reject│ │Toggle│     │ ← tappable items in a flow/grid
│  │  m   │ │      │ │  On  │     │   wrap to fill width
│  └──────┘ └──────┘ └──────┘     │
│  ┌──────┐ ┌──────┐ ┌──────┐     │
│  │Toggle│ │Long  │ │Kbd   │     │
│  │ Off  │ │Press │ │Press │     │
│  └──────┘ └──────┘ └──────┘     │
│  ┌──────┐ ┌──────┐ ┌──────┐     │
│  │Kbd   │ │Clock │ │Contxt│     │
│  │Relse │ │Tick  │ │Click │     │
│  └──────┘ └──────┘ └──────┘     │
│  ... (more items wrap as needed) │
│                                  │
│                                  │
│  PREDEFINED EFFECTS              │ ← section label
│                                  │
│  ┌──────┐ ┌──────┐ ┌──────┐     │
│  │Click │ │Double│ │ Tick │     │
│  │      │ │Click │ │      │     │
│  └──────┘ └──────┘ └──────┘     │
│  ┌──────┐                        │
│  │Heavy │                        │
│  │Click │                        │
│  └──────┘                        │
│                                  │
│                                  │
│  PRIMITIVES                      │ ← section label
│                                  │
│  ┌────────────────────────┐      │
│  │ Click          ◎ ┃██░░│      │ ← item with wheel + power bar
│  └────────────────────────┘      │
│  ┌────────────────────────┐      │
│  │ Tick           ◎ ┃█░░░│      │
│  └────────────────────────┘      │
│  ┌────────────────────────┐      │
│  │ Low Tick       ◎ ┃███░│      │
│  └────────────────────────┘      │
│  ┌────────────────────────┐      │
│  │ Quick Rise     ◎ ┃██░░│      │
│  └────────────────────────┘      │
│  ... (more primitives)           │
│                                  │
│                                  │
│  PATTERN                         │ ← section label
│                                  │
│  ┌────────────────────────┐      │
│  │ + Add primitive         │      │ ← tap to add from supported list
│  │                         │      │
│  │ ┌─────────────────────┐ │      │
│  │ │ Click  0.8  20ms  ✕ │ │      │ ← added primitive row
│  │ └─────────────────────┘ │      │
│  │ ┌─────────────────────┐ │      │
│  │ │ Thud   0.5  50ms  ✕ │ │      │ ← scale, delay, remove
│  │ └─────────────────────┘ │      │
│  │                         │      │
│  │       [ ▶ PLAY ]        │      │ ← play composed pattern
│  └────────────────────────┘      │
│                                  │
│                                  │
│  Not available on this device:   │ ← small grey text
│  Toggle On, Toggle Off,          │
│  Drag Start, PRIMITIVE_SPIN      │
│                                  │
└──────────────────────────────────┘
```

### Layout Rules

- **Sections 1 & 2** (Haptic Feedback, Predefined Effects): Items arranged in a **flow layout** (wrapping grid). Items are equal-sized rectangles fitting 3 per row with even spacing. Tap anywhere on the item to trigger.
- **Section 3** (Primitives): **Vertical list**. Each row has the primitive name on the left, a scrollable wheel control + power indicator on the right. Tap the row to trigger at the current scale. Scroll the wheel to adjust scale.
- **Section 4** (Pattern Builder): **Contained area**. Add button to insert primitives from a popup/dropdown of supported ones. Each added primitive shows as a row with scale, delay, and remove button. Play button at bottom.
- **Section 5** (Device Info): **Small grey monospace text**, left-aligned, at the very bottom. Lists names of hidden/unsupported effects.
- **Spacing**: ~24dp between sections, ~8dp between items within a section.
- **Padding**: ~16dp horizontal screen padding.
