# 12 – More scroll wheel inertia

## Feedback
The horizontal scroll wheel needs more inertia — a longer tail after release.

## Tickets

### UX-UI
**Status**: Pending

Current: friction=0.92, minVelocity=0.5dp. Friction 0.92 means velocity drops to ~37% after 60 frames (1s). Need a longer tail: increase friction to ~0.96 (velocity drops to ~8.5% after 60 frames) and lower minVelocity to 0.2dp.

### Development
**Status**: Pending

### Review
**Status**: Pending
