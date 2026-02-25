package vadiole.tremor

import android.os.VibrationEffect
import android.view.HapticFeedbackConstants
import vadiole.tremor.view.WaveOverlayView

fun hapticConstantStrength(constant: Int): Float = when (constant) {
    HapticFeedbackConstants.LONG_PRESS,
    HapticFeedbackConstants.CONFIRM,
    HapticFeedbackConstants.REJECT -> 0.8f
    HapticFeedbackConstants.KEYBOARD_PRESS,
    HapticFeedbackConstants.VIRTUAL_KEY -> 0.5f
    HapticFeedbackConstants.CLOCK_TICK,
    HapticFeedbackConstants.CONTEXT_CLICK,
    HapticFeedbackConstants.TEXT_HANDLE_MOVE,
    HapticFeedbackConstants.KEYBOARD_RELEASE,
    HapticFeedbackConstants.VIRTUAL_KEY_RELEASE -> 0.3f
    else -> 0.5f
}

fun effectStrength(effectId: Int): Float = when (effectId) {
    VibrationEffect.EFFECT_HEAVY_CLICK -> 1.0f
    VibrationEffect.EFFECT_DOUBLE_CLICK -> 0.7f
    VibrationEffect.EFFECT_CLICK -> 0.5f
    VibrationEffect.EFFECT_TICK -> 0.3f
    else -> 0.5f
}

fun primitiveWaveStyle(primitiveId: Int): WaveOverlayView.WaveStyle = when (primitiveId) {
    VibrationEffect.Composition.PRIMITIVE_CLICK -> WaveOverlayView.WaveStyle.CLICK
    VibrationEffect.Composition.PRIMITIVE_TICK,
    VibrationEffect.Composition.PRIMITIVE_LOW_TICK -> WaveOverlayView.WaveStyle.TICK
    VibrationEffect.Composition.PRIMITIVE_QUICK_RISE -> WaveOverlayView.WaveStyle.RISE
    VibrationEffect.Composition.PRIMITIVE_SLOW_RISE -> WaveOverlayView.WaveStyle.SLOW_RISE
    VibrationEffect.Composition.PRIMITIVE_QUICK_FALL -> WaveOverlayView.WaveStyle.FALL
    VibrationEffect.Composition.PRIMITIVE_SPIN -> WaveOverlayView.WaveStyle.SPIN
    VibrationEffect.Composition.PRIMITIVE_THUD -> WaveOverlayView.WaveStyle.THUD
    else -> WaveOverlayView.WaveStyle.DEFAULT
}
