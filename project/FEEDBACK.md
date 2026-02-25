# Iteration 3 – Feedback Items

| # | Title | Type | Status |
|---|---|---|---|
| 1 | Density interface for dp calculations | dev | Done |
| 2 | More example elements (top 7) | research + validate + dev | Done |
| 3 | Improve WaveOverlayView per-primitive character | research + dev | Done |
| 4 | Fix DrumRollerView haptics to SEGMENT_FREQUENT_TICK | dev | Done |
| 5 | Fix Play Store link, underline, press effect | dev | Done |
| 6 | Fix system bar colors in light mode | dev | Done |
| 7 | Improve toggle: animation, taller, full-row tap | dev | Done |
| 8 | Fix wave colors in light mode | dev | Pending |
| 9 | Research subtle ambient wave effect | research + dev (optional) | Pending |
| 10 | Research additional visual feedback ideas | research + validate | Pending |
| 11 | No raw strings rule + enforce | dev + spec | Pending |

---

## 1. Density interface for dp calculations

**Type**: dev

Create `interface Density { fun getResources(): Resources; fun Float.dp(): Float; fun Int.dp(): Int }` and apply it to all custom views to replace manual `* density` calculations everywhere.

---

## 2. More example elements (top 7)

**Type**: research + validate + dev

Explore which interactive example components best showcase different haptic types. Need top 7. Validate ideas with user before implementing. Examples to consider:
- Long click with visual feedback (button shrinking)
- Horizontal scroll wheel with inertia
- Rise/fall effect demo (like Google Circle to Search — gradient overlay + haptic rise)
- Keyboard press/release simulation
- Each example should demonstrate a different interaction type where the haptic character matters

---

## 3. Improve WaveOverlayView per-primitive character

**Type**: research + dev

Currently responsive to amplitude but not to the character of the haptic itself. Primitives like `rise`, `fall`, `spin`, `thud` need thicker, more distinctive waves. `slow_rise` should have a delayed/slower wave expansion. Figure out best balance for all primitives and effects.

---

## 4. Fix DrumRollerView haptics to SEGMENT_FREQUENT_TICK

**Type**: dev

Replace `CLOCK_TICK` with `SEGMENT_FREQUENT_TICK` in DrumRollerView scroll haptics.

---

## 5. Fix Play Store link, underline, press effect

**Type**: dev

- Fix URL to: `https://play.google.com/store/apps/dev?id=4763171503902347202`
- Only "vadiole" should be the link text (not the whole footer)
- Add underline decoration
- Add press effect: alpha 50% on press

---

## 6. Fix system bar colors in light mode

**Type**: dev

System bars are white on white in light mode. Fix status bar and navigation bar appearance for light theme (dark icons on light background).

---

## 7. Improve toggle: animation, taller, full-row tap

**Type**: dev

- Add smooth animation to toggle thumb movement
- Make toggle taller to match Material3/iOS 16 proportions
- Make the entire row tappable to toggle (not just the switch)

---

## 8. Fix wave colors in light mode

**Type**: dev

Waves are almost invisible in light mode. Need darker/more visible wave color for light theme.

---

## 9. Research subtle ambient wave effect

**Type**: research + dev (optional)

Research possibility of a very small ambient wave effect layered on top of existing waves — warms the space subtly without making users dizzy. Must be high performance. Optional if some devices can't support it.

---

## 10. Research additional visual feedback ideas

**Type**: research + validate

Deep research: generate ideas, validate, improve, generate more. Goal: solid ideas for visual feedback. Inspiration:
- Minimal particles as easter egg
- Feedback for long-duration interactions (dragging wheel, waiting for rise/fall)
- Other creative visual responses to haptic events

---

## 11. No raw strings rule + enforce

**Type**: dev + spec

Ensure zero raw strings in codebase. Add rule to tech spec / project constraints so this never happens again.
