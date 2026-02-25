package vadiole.tremor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

class HapticEngine(context: Context) {

    private val vibrator: Vibrator = run {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    }

    val hasVibrator: Boolean = vibrator.hasVibrator()

    fun isHapticEnabled(view: View): Boolean {
        // Must be called when the view is attached (e.g. from onWindowFocusChanged),
        // because performHapticFeedback returns false when mAttachInfo is null.
        // On API 35+ the async path always returns true, so the banner won't
        // show a false positive — acceptable for a haptics testing app.
        return view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun getAvailableHapticConstants(): List<HapticConstantInfo> {
        val api = Build.VERSION.SDK_INT
        return allHapticConstants.filter { it.minApi <= api }
    }

    fun getUnavailableHapticConstants(): List<HapticConstantInfo> {
        val api = Build.VERSION.SDK_INT
        return allHapticConstants.filter { it.minApi > api }
    }

    fun getSupportedEffects(): List<EffectInfo> {
        val support = vibrator.areEffectsSupported(
            VibrationEffect.EFFECT_CLICK,
            VibrationEffect.EFFECT_DOUBLE_CLICK,
            VibrationEffect.EFFECT_TICK,
            VibrationEffect.EFFECT_HEAVY_CLICK,
        )
        return allEffects.filterIndexed { index, _ ->
            support[index] != Vibrator.VIBRATION_EFFECT_SUPPORT_NO
        }
    }

    fun getUnsupportedEffects(): List<EffectInfo> {
        val support = vibrator.areEffectsSupported(
            VibrationEffect.EFFECT_CLICK,
            VibrationEffect.EFFECT_DOUBLE_CLICK,
            VibrationEffect.EFFECT_TICK,
            VibrationEffect.EFFECT_HEAVY_CLICK,
        )
        return allEffects.filterIndexed { index, _ ->
            support[index] == Vibrator.VIBRATION_EFFECT_SUPPORT_NO
        }
    }

    fun getSupportedPrimitives(): List<PrimitiveInfo> {
        val ids = allPrimitives.map { it.primitiveId }.toIntArray()
        val support = vibrator.arePrimitivesSupported(*ids)
        return allPrimitives.filterIndexed { index, _ -> support[index] }
    }

    fun getUnsupportedPrimitives(): List<PrimitiveInfo> {
        val ids = allPrimitives.map { it.primitiveId }.toIntArray()
        val support = vibrator.arePrimitivesSupported(*ids)
        return allPrimitives.filterIndexed { index, _ -> !support[index] }
    }

    fun playEffect(effectId: Int) {
        val effect = VibrationEffect.createPredefined(effectId)
        vibrator.vibrate(effect)
    }

    fun playPrimitive(primitiveId: Int, scale: Float) {
        try {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(primitiveId, scale)
                .compose()
            vibrator.vibrate(effect)
        } catch (_: Exception) {
        }
    }

    fun cancel() {
        vibrator.cancel()
    }

    data class HapticConstantInfo(
        val nameResId: Int,
        val constantName: String,
        val value: Int,
        val minApi: Int,
    )

    data class EffectInfo(
        val nameResId: Int,
        val constantName: String,
        val effectId: Int,
    )

    data class PrimitiveInfo(
        val nameResId: Int,
        val constantName: String,
        val primitiveId: Int,
    )

    companion object {

        val allHapticConstants = listOf(
            HapticConstantInfo(R.string.haptic_confirm, "CONFIRM", HapticFeedbackConstants.CONFIRM, 30),
            HapticConstantInfo(R.string.haptic_reject, "REJECT", HapticFeedbackConstants.REJECT, 30),
            HapticConstantInfo(R.string.haptic_toggle_on, "TOGGLE_ON", 21, 34),
            HapticConstantInfo(R.string.haptic_toggle_off, "TOGGLE_OFF", 22, 34),
            HapticConstantInfo(R.string.haptic_long_press, "LONG_PRESS", HapticFeedbackConstants.LONG_PRESS, 3),
            HapticConstantInfo(R.string.haptic_keyboard_press, "KEYBOARD_PRESS", HapticFeedbackConstants.KEYBOARD_PRESS, 27),
            HapticConstantInfo(R.string.haptic_keyboard_release, "KEYBOARD_RELEASE", HapticFeedbackConstants.KEYBOARD_RELEASE, 27),
            HapticConstantInfo(R.string.haptic_clock_tick, "CLOCK_TICK", HapticFeedbackConstants.CLOCK_TICK, 21),
            HapticConstantInfo(R.string.haptic_context_click, "CONTEXT_CLICK", HapticFeedbackConstants.CONTEXT_CLICK, 23),
            HapticConstantInfo(R.string.haptic_gesture_start, "GESTURE_START", 12, 30),
            HapticConstantInfo(R.string.haptic_gesture_end, "GESTURE_END", 13, 30),
            HapticConstantInfo(R.string.haptic_gesture_threshold_activate, "GESTURE_THRESHOLD_ACTIVATE", 23, 34),
            HapticConstantInfo(R.string.haptic_gesture_threshold_deactivate, "GESTURE_THRESHOLD_DEACTIVATE", 24, 34),
            HapticConstantInfo(R.string.haptic_text_handle_move, "TEXT_HANDLE_MOVE", HapticFeedbackConstants.TEXT_HANDLE_MOVE, 27),
            HapticConstantInfo(R.string.haptic_virtual_key, "VIRTUAL_KEY", HapticFeedbackConstants.VIRTUAL_KEY, 3),
            HapticConstantInfo(R.string.haptic_virtual_key_release, "VIRTUAL_KEY_RELEASE", HapticFeedbackConstants.VIRTUAL_KEY_RELEASE, 27),
            HapticConstantInfo(R.string.haptic_drag_start, "DRAG_START", 25, 34),
            HapticConstantInfo(R.string.haptic_segment_tick, "SEGMENT_TICK", 26, 34),
            HapticConstantInfo(R.string.haptic_segment_frequent_tick, "SEGMENT_FREQUENT_TICK", 27, 34),
        )

        val allEffects = listOf(
            EffectInfo(R.string.effect_click, "EFFECT_CLICK", VibrationEffect.EFFECT_CLICK),
            EffectInfo(R.string.effect_double_click, "EFFECT_DOUBLE_CLICK", VibrationEffect.EFFECT_DOUBLE_CLICK),
            EffectInfo(R.string.effect_tick, "EFFECT_TICK", VibrationEffect.EFFECT_TICK),
            EffectInfo(R.string.effect_heavy_click, "EFFECT_HEAVY_CLICK", VibrationEffect.EFFECT_HEAVY_CLICK),
        )

        val allPrimitives = listOf(
            PrimitiveInfo(R.string.primitive_click, "PRIMITIVE_CLICK", VibrationEffect.Composition.PRIMITIVE_CLICK),
            PrimitiveInfo(R.string.primitive_tick, "PRIMITIVE_TICK", VibrationEffect.Composition.PRIMITIVE_TICK),
            PrimitiveInfo(R.string.primitive_low_tick, "PRIMITIVE_LOW_TICK", VibrationEffect.Composition.PRIMITIVE_LOW_TICK),
            PrimitiveInfo(R.string.primitive_quick_rise, "PRIMITIVE_QUICK_RISE", VibrationEffect.Composition.PRIMITIVE_QUICK_RISE),
            PrimitiveInfo(R.string.primitive_slow_rise, "PRIMITIVE_SLOW_RISE", VibrationEffect.Composition.PRIMITIVE_SLOW_RISE),
            PrimitiveInfo(R.string.primitive_quick_fall, "PRIMITIVE_QUICK_FALL", VibrationEffect.Composition.PRIMITIVE_QUICK_FALL),
            PrimitiveInfo(R.string.primitive_spin, "PRIMITIVE_SPIN", VibrationEffect.Composition.PRIMITIVE_SPIN),
            PrimitiveInfo(R.string.primitive_thud, "PRIMITIVE_THUD", VibrationEffect.Composition.PRIMITIVE_THUD),
        )
    }
}
