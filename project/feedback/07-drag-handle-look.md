# 07 – Improve draggable handle appearance

**Problem**: The current draggable handle in DragThresholdView looks outdated. Needs a modern, minimal look.

---

## Ticket 1: UX-UI

**Status**: Pending

Review the current handle design: solid foreground-colored rounded rect with 3 vertical grip lines in background color. Research modern drag handle patterns — consider:
- Pill-shaped handle with subtle border instead of solid fill
- Horizontal grip lines instead of vertical (more common for horizontal drag)
- Lighter fill with stronger border
- Rounded pill shape (more rounded corners)
- Arrow/chevron indicator showing drag direction
- Match the overall minimal black/white aesthetic

Recommend the best approach.

---

## Ticket 2: Development

**Status**: Pending

Redesign the handle based on UX-UI recommendation. Update DragThresholdView drawing code.

---

## Ticket 3: Review

**Status**: Pending

Verify the handle looks modern and consistent with the app aesthetic. Check it looks good in both light and dark mode. Ensure touch area is still generous.
