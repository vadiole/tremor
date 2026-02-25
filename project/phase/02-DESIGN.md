# Phase 2 – Design

## Ticket Status

| ID | Title | Status |
|---|---|---|
| DES-01 | Screen Structure & Layout | Done |
| DES-02 | Element Design | Done |
| DES-03 | Visual Feedback Strategy | Done |

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
│  ┌──────────────────────────┐    │
│  │ Click       .60  ┌───┐  │    │ ← drum roller + number
│  │                   │───│  │    │
│  │                   └───┘  │    │
│  └──────────────────────────┘    │
│  ┌──────────────────────────┐    │
│  │ Tick        .80  ┌───┐  │    │
│  │                   │───│  │    │
│  │                   └───┘  │    │
│  └──────────────────────────┘    │
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

Each primitive has a name, a drum roller for scale, a power fill bar, and a numeric readout.

```
┌──────────────────────────────────────┐
│                                      │  height: 64dp
│                              ┌───┐   │  background: #1A1A1A
│                              │───│   │  border: 1px #333
│  Click                  .60  │───│   │  corner radius: 6dp
│                              │───│   │
│                              └───┘   │
│                                      │
└──────────────────────────────────────┘

  ↑                        ↑    ↑
label                   number  drum
```

- **Label**: Left-aligned, monospace 13sp, white
- **Drum roller**: 20dp wide × 48dp tall. Displays only horizontal lines (───) — no numbers on the drum. Lines scroll vertically as user drags up/down. The drum is a clipped window showing ~5 horizontal lines at varying opacity — center line is brightest, lines near edges fade to transparent. Scrolling adjusts scale 0.0–1.0 in 0.05 increments (21 steps). Visual feel: like scrolling a mechanical counter.
  - Lines: 1px white, horizontal, full width of drum, spaced 8dp apart vertically
  - Clipping: Rounded rect mask on the drum area
  - Edge fade: Lines near top/bottom edges fade to transparent
  - Border: Subtle 1px #333 around the drum window
- **Number readout**: Monospace 11sp, #888 (grey), positioned to the left of the drum. Shows "0.60" format (always 2 decimal places). Updates as drum scrolls.
- **Interaction**:
  - Tap anywhere on the row (outside drum) → trigger primitive at current scale + wave animation
  - Vertical drag/scroll on drum area → adjust scale (haptic tick on each 0.05 step)
  - Drum touch does NOT trigger the haptic — only adjusts scale
- **Default scale**: 1.0 (full power) for all primitives on launch

### Element Type C: Pattern Builder (Section 4)

A contained area for composing sequences of primitives.

```
┌──────────────────────────────────────┐
│                                      │
│  ┌────────────────────────────────┐  │
│  │ Click .80 ┌──┐ 20ms ┌──┐  ✕  │  │  primitive row: 48dp
│  │           │──│      │──│      │  │  two drums: scale + delay
│  │           └──┘      └──┘      │  │
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │ Thud  .50 ┌──┐ 50ms ┌──┐  ✕  │  │
│  │           │──│      │──│      │  │
│  │           └──┘      └──┘      │  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │           + ADD                │  │  add button: dashed border
│  └────────────────────────────────┘  │
│                                      │
│           ┌──────────┐               │
│           │  ▶ PLAY  │               │  play button: white border
│           └──────────┘               │  centered, 48dp height
│                                      │
└──────────────────────────────────────┘
```

- **Primitive row**: Shows name, scale drum, delay drum, and ✕ remove button
  - Scale drum: Same drum roller as Element Type B — horizontal lines, 0.0–1.0 in 0.05 steps. Number readout to the left.
  - Delay drum: Same drum style — horizontal lines, 0–500ms in 10ms steps. Number readout ("20ms") to the left.
  - Remove (✕): 24dp tap target, grey #555, turns white on press
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

---

## Visual Feedback Strategy (DES-03 Output)

### Concept: Ripple Wave

Every haptic trigger creates a **circular wave** that expands outward from the exact touch point. The wave is a translucent gradient ring that grows and fades — like dropping a stone in still water.

### Wave Behavior

- **Origin**: The exact (x, y) coordinates of the touch event, in screen-absolute coordinates.
- **Shape**: Circular ring with a soft Gaussian-like cross-section profile — no hard edges.
- **Expansion**: The ring expands outward at a constant velocity (~800dp/s). Starts at radius 0, grows until it exits the screen bounds.
- **Fade**: Overall intensity fades smoothly over the wave's lifetime using ease-out (not linear). Soft start, soft end.
- **Ring width**: ~40dp wide soft gradient band. Cross-section profile: `0.0 → smooth ease-in → peak → smooth ease-out → 0.0`. Achieved via `smoothstep` in shader. No abrupt transitions anywhere.
- **Duration**: ~600ms from spawn to fully faded (depends on screen size; wave travels off-screen).
- **Color**: White (#FFFFFF) with alpha. Translucent gradient on black background creates a soft, natural glow.

### Multiple Waves & Interference

When the user taps rapidly or drags (e.g., scrolling a wheel that fires haptic ticks), multiple waves coexist:

- Each haptic trigger spawns an independent wave at its touch coordinates.
- Waves do NOT cancel each other — they **additively blend**. Overlapping regions appear brighter.
- This creates natural interference patterns when taps are rapid or when dragging across items.
- Max concurrent waves: ~10 (oldest waves auto-removed when limit reached to preserve performance).

### Implementation Approach

**AGSL RuntimeShader** rendered on a full-screen `View` overlay.

- `RuntimeShader` receives all active wave data as uniforms (origins array, radii array, intensities array, wave count).
- GPU computes per-pixel brightness: for each wave, calculate distance from fragment to wave origin, apply `smoothstep` to produce a soft bell-curve ring, multiply by intensity, and sum additively.
- True additive blending — overlapping waves produce natural interference brightness.
- Performance: Excellent even with 10+ concurrent waves, all computed in a single shader pass.
- Fallback for API 31–32 (no `RuntimeShader`): Canvas-based `RadialGradient` with multi-stop color array approximating the smooth profile, using `BlendMode.SCREEN`.

### Wave Lifecycle

```
t=0ms    Touch down detected, haptic fires
         → Spawn new wave at (touchX, touchY)
         → radius=0, intensity=1.0

t=0-600  Expanding
         → radius increases at ~800dp/s
         → intensity fades via ease-out curve (smooth deceleration to 0)
         → ring width stays constant at ~40dp
         → ring profile: smooth bell curve (smoothstep in, smoothstep out)

t=600    Wave complete
         → Remove from active wave list
```

### Integration Points

- **HapticButton tap** (Sections 1 & 2): Wave spawns at the tap coordinates within the button, translated to screen coordinates.
- **PrimitiveRow tap** (Section 3): Wave spawns at tap point on the row.
- **Wheel scroll** (Section 3): Each scale step change fires a haptic tick → wave spawns at the wheel's screen position.
- **Pattern play** (Section 4): Wave spawns at the play button location for each primitive in the sequence (with timing matching the composition delays).

### Overlay Architecture

```
FrameLayout (root)
├── ScrollView
│   └── LinearLayout (all sections)
└── WaveOverlayView (match_parent, clickable=false)
    └── intercepts no touches, only draws waves
```

- `WaveOverlayView` sits on top of the scroll content.
- It is **not clickable** — all touches pass through to the content below.
- Other views call `waveOverlay.spawnWave(screenX, screenY)` when a haptic fires.
- The overlay animates independently using `invalidate()` loop during active waves.
