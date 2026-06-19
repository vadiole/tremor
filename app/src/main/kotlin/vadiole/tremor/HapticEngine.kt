package vadiole.tremor

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants

class HapticEngine(
    context: Context,
    private val viewHapticFeedback: (feedbackConstant: Int) -> Unit,
) {

    private val appContext: Context = context.applicationContext

    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val vibrator: Vibrator = run {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    }

    // Which control the user turned off, when distinguishable. MASTER = the "Use vibration
    // & haptics" switch (VIBRATE_ON); TOUCH = the touch-feedback toggle/slider. Reliable only
    // on stock Android (Pixel/Motorola): heavy OEM skins don't keep VIBRATE_ON in sync — e.g.
    // Samsung One UI never writes it (uses SEM_VIBRATION_*), so master-off is invisible here.
    enum class HapticsOffCause { MASTER, TOUCH }

    class GateState(
        val systemHapticsOn: Boolean,
        val powerSaveOn: Boolean,
        val constantsMuted: Boolean,
        val effectsMuted: Boolean,
        val offCause: HapticsOffCause?,
    )

    // Every gate that can mute this app, by playback path (fail open — predict "muted"
    // only on gates the service is known to enforce; verified on API 31 & 35, AOSP source
    // VibrationSettings.java):
    // - VIBRATE_ON: master switch; gates the constants path on API 33+ only.
    // - HAPTIC_FEEDBACK_ENABLED: gates the constants path (View.performHapticFeedback).
    // - haptic_feedback_intensity: touch-intensity slider; gates the constants path on
    //   every API and the effects path on 31/32 (no public constant; absent = enabled).
    // - USAGE_ACCESSIBILITY: effects/primitives bypass all three keys on API 33+; on
    //   31/32 effects fall back to the intensity slider and primitives stay ungated.
    // - Battery saver: may mute everything incl. the bypass, but only via an unreadable
    //   policy flag — surfaced as a hedged banner cause, never a per-tap prediction.
    @Suppress("DEPRECATION")
    fun readGateState(): GateState {
        val resolver = appContext.contentResolver
        val masterOn = Settings.System.getInt(resolver, Settings.System.VIBRATE_ON, 1) != 0
        val touchOn = Settings.System.getInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
        val intensityOn = Settings.System.getInt(resolver, "haptic_feedback_intensity", 1) != 0
        val masterGated = Build.VERSION.SDK_INT >= 33
        val systemOn = if (masterGated) {
            masterOn && touchOn && intensityOn
        } else {
            touchOn && intensityOn
        }
        return GateState(
            systemHapticsOn = systemOn,
            powerSaveOn = powerManager.isPowerSaveMode,
            constantsMuted = !systemOn,
            effectsMuted = Build.VERSION.SDK_INT < 33 && !intensityOn,
            offCause = when {
                systemOn -> null
                masterGated && !masterOn -> HapticsOffCause.MASTER
                else -> HapticsOffCause.TOUCH
            },
        )
    }

    fun performHapticFeedback(feedbackConstant: Int) {
        viewHapticFeedback(feedbackConstant)
    }

    fun getAvailableHapticConstants(): List<HapticConstantInfo> {
        val api = Build.VERSION.SDK_INT
        return allHapticConstants.filter { it.minApi <= api }
    }

    fun getUnavailableHapticConstants(): List<HapticConstantInfo> {
        val api = Build.VERSION.SDK_INT
        return allHapticConstants.filter { it.minApi > api }
    }

    fun getAllEffects(): List<EffectInfo> = allEffects

    fun getFallbackEffectIds(): Set<Int> {
        val ids = allEffects.map { it.effectId }.toIntArray()
        val support = vibrator.areEffectsSupported(*ids)
        return allEffects.filterIndexed { index, _ ->
            support[index] == Vibrator.VIBRATION_EFFECT_SUPPORT_NO
        }.map { it.effectId }.toSet()
    }

    fun getSupportedPrimitives(): List<PrimitiveInfo> {
        val ids = allPrimitives.map { it.primitiveId }.toIntArray()
        val support = vibrator.arePrimitivesSupported(*ids)
        return allPrimitives.filterIndexed { index, _ -> support[index] }
    }

    // USAGE_ACCESSIBILITY is the one usage the vibrator plays even when the master
    // "Use vibration & haptics" (VIBRATE_ON) switch is off
    private val accessibilityAttributes: VibrationAttributes? = if (Build.VERSION.SDK_INT >= 33) {
        VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_ACCESSIBILITY)
            .build()
    } else {
        null
    }

    private fun vibrate(effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, accessibilityAttributes!!)
        } else {
            vibrator.vibrate(effect)
        }
    }

    fun playEffect(effectId: Int) {
        val effect = VibrationEffect.createPredefined(effectId)
        vibrate(effect)
    }

    fun playPrimitive(primitiveId: Int, scale: Float) {
        try {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(primitiveId, scale)
                .compose()
            vibrate(effect)
        } catch (_: Exception) {
        }
    }

    fun cancel() {
        vibrator.cancel()
    }

    class HapticConstantInfo(
        val nameResId: Int,
        val constantName: String,
        val value: Int,
        val minApi: Int,
    )

    class EffectInfo(
        val nameResId: Int,
        val constantName: String,
        val effectId: Int,
    )

    class PrimitiveInfo(
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
