# 11 – QWERTY buttons scale-down press effect

## Feedback
QWERTY keyboard buttons must have the same press scale-down/up effect as other clickable elements.

## Tickets

### UX-UI
**Status**: Pending

Individual key scale isn't possible with a single-View approach (Canvas-drawn keys). Instead, scale the entire KeyboardRowView on press/release, matching the 0.97x/80ms down + 1.0x/150ms/OvershootInterpolator(2f) pattern.

### Development
**Status**: Pending

### Review
**Status**: Pending
