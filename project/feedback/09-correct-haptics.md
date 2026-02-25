# 09 – Correct haptics for all example elements

## Feedback
Every example element should use the most appropriate haptic constant for its interaction type.

## Audit
- **HapticToggle**: Uses CONFIRM/REJECT → should use TOGGLE_ON(21)/TOGGLE_OFF(22)
- **DragThresholdView**: Uses GESTURE_START/GESTURE_END/CONFIRM/CLOCK_TICK → should use DRAG_START(25) for gesture start, GESTURE_THRESHOLD_ACTIVATE(23)/GESTURE_THRESHOLD_DEACTIVATE(24) for threshold crossing
- **LongPressButton**: Uses LONG_PRESS + CLOCK_TICK — correct
- **HapticCounter**: Uses CLOCK_TICK — correct
- **KeyboardRowView**: Uses KEYBOARD_PRESS/KEYBOARD_RELEASE — correct
- **ScrollWheelView**: Uses SEGMENT_FREQUENT_TICK — correct
- **RiseFallButton**: Uses PRIMITIVE_QUICK_RISE/QUICK_FALL via Vibrator — correct

## Tickets

### UX-UI
**Status**: Done

Toggle should use TOGGLE_ON/TOGGLE_OFF (API 34, values 21/22). These are the purpose-built constants.
DragThreshold should use DRAG_START (API 34, value 25) for drag begin, GESTURE_THRESHOLD_ACTIVATE/DEACTIVATE (API 34, values 23/24) for threshold crossing. GESTURE_END stays for drag end.

All these constants already exist in the haptic constants catalog. On API < 34 they'll be no-ops (performHapticFeedback returns false for unknown constants), which is acceptable since the examples are educational.

### Development
**Status**: Pending

Update HapticToggle to use TOGGLE_ON(21)/TOGGLE_OFF(22).
Update DragThresholdView to use DRAG_START(25) and GESTURE_THRESHOLD_ACTIVATE(23)/DEACTIVATE(24).

### Review
**Status**: Pending
