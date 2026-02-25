# 03 – Rollback bad feedback additions

**Problem**: Three recent additions are bad and should be removed:
1. **Long press radial arc** — the arc progress on LongPressButton
2. **Drag trail afterimages** — ghost images trailing DragThresholdView handle
3. **Ambient noise deformation** — perlin noise ambient effect in WaveOverlayView

---

## Ticket 1: UX-UI

**Status**: Pending

Identify all code added for these three features so rollback is clean and complete. List exact code blocks and files affected.

---

## Ticket 2: Development

**Status**: Pending

Remove:
- LongPressButton: remove `arcPaint`, `arcRect`, and the `drawArc` block in `onDraw`
- DragThresholdView: remove `ghostPaint`, `ghostRect`, trail ring buffer (`trailX`, `trailY`, `trailIndex`, `trailSize`), `recordTrail()`, `clearTrail()`, trail drawing in `onDraw`, `recordTrail()` calls in touch/spring
- WaveOverlayView: remove ambient noise — `ambientRunning`, `startAmbient()`, `stopAmbient()`, the `vnoise`/`hash2` functions in shader, `time`/`ambientEnabled` uniforms, ambient drawing logic in `onDraw`
- TremorActivity: remove `startAmbient()`/`stopAmbient()` calls

---

## Ticket 3: Review

**Status**: Pending

Verify all three features are fully removed. Build clean. No leftover dead code, unused variables, or orphaned uniforms in the shader. Wave overlay still works for haptic-triggered waves. Long press and drag threshold still function correctly without the removed visuals.
