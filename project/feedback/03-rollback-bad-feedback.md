# 03 – Rollback bad feedback additions

**Problem**: Three recent additions are bad and should be removed:
1. **Long press radial arc** — the arc progress on LongPressButton
2. **Drag trail afterimages** — ghost images trailing DragThresholdView handle
3. **Ambient noise deformation** — perlin noise ambient effect in WaveOverlayView

---

## Ticket 1: UX-UI

**Status**: Done

Identified all code to remove across 4 files: LongPressButton (arc), DragThresholdView (trail), WaveOverlayView (ambient noise + uniforms + shader functions), TremorActivity (ambient lifecycle).

---

## Ticket 2: Development

**Status**: Done

Remove:
- LongPressButton: remove `arcPaint`, `arcRect`, and the `drawArc` block in `onDraw`
- DragThresholdView: remove `ghostPaint`, `ghostRect`, trail ring buffer (`trailX`, `trailY`, `trailIndex`, `trailSize`), `recordTrail()`, `clearTrail()`, trail drawing in `onDraw`, `recordTrail()` calls in touch/spring
- WaveOverlayView: remove ambient noise — `ambientRunning`, `startAmbient()`, `stopAmbient()`, the `vnoise`/`hash2` functions in shader, `time`/`ambientEnabled` uniforms, ambient drawing logic in `onDraw`
- TremorActivity: remove `startAmbient()`/`stopAmbient()` calls

---

## Ticket 3: Review

**Status**: Done

- Build clean, no warnings.
- Grep for all removed identifiers (ambient, ghost, trail, arc, vnoise, hash2) returns zero hits.
- Shader has no orphaned uniforms — `time` and `ambientEnabled` removed.
- Wave overlay still handles haptic-triggered waves and touch dots correctly.
- LongPressButton retains its progress bar. DragThresholdView retains spring animation.
