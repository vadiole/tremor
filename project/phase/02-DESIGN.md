# Phase 2 – Design

## Ticket Status

| ID | Title | Status |
|---|---|---|
| DES-01 | Screen Structure & Layout | Done |
| DES-02 | Element Design | Done |
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

---

## Element Design (DES-02 Output)

### Element Type A: Haptic Button (Sections 1 & 2)

Used for HapticFeedbackConstants and Predefined Effects — simple tap-to-trigger items.

```
┌─────────────┐
│             │  height: 48dp
│   Confirm   │  monospace font, 13sp, white
│             │  background: #1A1A1A (very dark grey)
└─────────────┘  border: 1px #333333 (subtle)
                 corner radius: 6dp
```

- **Idle state**: Dark grey background (#1A1A1A), subtle border (#333), white mono text centered
- **Pressed state**: Background lightens to #2A2A2A, text stays white
- **Touch target**: Entire rectangle
- **Interaction**: Single tap → trigger haptic + fire wave animation from touch point
- **Flow layout**: Items sized to fit 3 per row with 8dp gaps. Text truncates with ellipsis if too long (shouldn't happen with short labels).

### Element Type B: Primitive Row (Section 3)

Each primitive has a name, a wheel control for scale, and a power fill indicator.

```
┌──────────────────────────────────┐
│                                  │  height: 56dp
│  Click                 ◎ ┃██░░  │  background: #1A1A1A
│                                  │  border: 1px #333
└──────────────────────────────────┘  corner radius: 6dp

     ↑                   ↑   ↑
   label              wheel  power bar
   (mono 13sp)        (24dp) (fill indicator)
```

- **Label**: Left-aligned, monospace 13sp, white
- **Wheel control** (◎): 24x24dp circular element on the right side. User scrolls vertically (up = increase, down = decrease) to adjust scale 0.0–1.0 in 0.05 steps. Visual: small circle with tick marks or a notch indicating rotation.
- **Power indicator** (┃██░░): Vertical bar or horizontal fill, 4dp wide x 40dp tall (or 40dp wide x 6dp tall). Fills proportionally to current scale. White fill on dark background.
- **Interaction**:
  - Tap anywhere on the row (except wheel) → trigger primitive at current scale + wave animation
  - Scroll on wheel area → adjust scale (haptic tick on each step change)
- **Scale display**: Small text showing current value like "0.75" next to the power bar, monospace 10sp, grey (#888)

### Element Type C: Pattern Builder (Section 4)

A contained area for composing sequences of primitives.

```
┌──────────────────────────────────┐
│  PATTERN                         │
│                                  │
│  ┌──────────────────────────┐    │
│  │ Click    0.8   20ms   ✕  │    │  primitive row: 40dp height
│  └──────────────────────────┘    │  background: #111
│  ┌──────────────────────────┐    │
│  │ Thud     0.5   50ms   ✕  │    │
│  └──────────────────────────┘    │
│                                  │
│  ┌──────────────────────────┐    │
│  │         + ADD             │    │  add button: dashed border
│  └──────────────────────────┘    │
│                                  │
│         ┌──────────┐             │
│         │  ▶ PLAY  │             │  play button: white border
│         └──────────┘             │  centered, 48dp height
│                                  │
└──────────────────────────────────┘
```

- **Primitive row**: Shows name, scale (editable via wheel), delay in ms (editable via wheel), and ✕ remove button
  - Scale wheel: same as Element Type B wheel
  - Delay wheel: scrollable, increments of 10ms, range 0–500ms
  - Remove (✕): 24dp tap target, grey, turns white on hover
- **Add button**: Dashed border (#333), monospace text "+ ADD". Tap opens a simple dropdown/popup listing supported primitives. Popup: black background, white text, each item is tappable.
- **Play button**: Centered, solid white border, monospace "▶ PLAY". Tap → compose and vibrate the full pattern + wave animation.
- **Limit**: Max 5 primitives in a pattern (add button hides at 5).
- **Empty state**: Just the add button and a disabled play button.

### Element Type D: Section Label

```
  HAPTIC FEEDBACK                    monospace 11sp
                                     color: #666 (grey)
                                     uppercase
                                     letter-spacing: 2dp
                                     margin-bottom: 12dp
```

### Element Type E: Device Info (Section 5)

```
  Not available on this device:      monospace 10sp
  Toggle On, Toggle Off,             color: #555 (dark grey)
  Drag Start, PRIMITIVE_SPIN         line-height: 16sp
                                     margin-top: 32dp
                                     margin-bottom: 24dp
```

### Typography

- **Font**: System monospace (`Typeface.MONOSPACE`)
- **Section labels**: 11sp, #666, uppercase, letter-spacing 2dp
- **Item labels**: 13sp, #FFF, normal case
- **Scale values**: 10sp, #888
- **Device info**: 10sp, #555

### Color Palette

| Token | Hex | Usage |
|---|---|---|
| Background | #000000 | Screen background |
| Surface | #1A1A1A | Item backgrounds |
| Surface pressed | #2A2A2A | Item press state |
| Border | #333333 | Item borders |
| Text primary | #FFFFFF | Labels, values |
| Text secondary | #888888 | Scale values, hints |
| Text muted | #666666 | Section labels |
| Text disabled | #555555 | Device info |
| Dashed border | #333333 | Add button in pattern builder |
