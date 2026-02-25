package vadiole.tremor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants

class HapticEngine(context: Context) {

    private val vibrator: Vibrator = run {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    }

    val hasVibrator: Boolean = vibrator.hasVibrator()

    fun isHapticEnabled(context: Context): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
        } catch (_: Exception) {
            true
        }
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
        val effect = VibrationEffect.startComposition()
            .addPrimitive(primitiveId, scale)
            .compose()
        vibrator.vibrate(effect)
    }

    fun playPattern(entries: List<PatternEntry>) {
        if (entries.isEmpty()) return
        val composition = VibrationEffect.startComposition()
        for (entry in entries) {
            composition.addPrimitive(entry.primitiveId, entry.scale, entry.delayMs)
        }
        vibrator.vibrate(composition.compose())
    }

    fun cancel() {
        vibrator.cancel()
    }

    data class HapticConstantInfo(
        val name: String,
        val constantName: String,
        val value: Int,
        val minApi: Int,
    )

    data class EffectInfo(
        val name: String,
        val constantName: String,
        val effectId: Int,
    )

    data class PrimitiveInfo(
        val name: String,
        val constantName: String,
        val primitiveId: Int,
    )

    data class PatternEntry(
        val primitiveId: Int,
        val name: String,
        val scale: Float,
        val delayMs: Int,
    )

    companion object {

        val allHapticConstants = listOf(
            HapticConstantInfo("Confirm", "CONFIRM", HapticFeedbackConstants.CONFIRM, 30),
            HapticConstantInfo("Reject", "REJECT", HapticFeedbackConstants.REJECT, 30),
            HapticConstantInfo("Toggle On", "TOGGLE_ON", 21, 34),
            HapticConstantInfo("Toggle Off", "TOGGLE_OFF", 22, 34),
            HapticConstantInfo("Long Press", "LONG_PRESS", HapticFeedbackConstants.LONG_PRESS, 3),
            HapticConstantInfo("Keyboard Press", "KEYBOARD_PRESS", HapticFeedbackConstants.KEYBOARD_PRESS, 27),
            HapticConstantInfo("Keyboard Release", "KEYBOARD_RELEASE", HapticFeedbackConstants.KEYBOARD_RELEASE, 27),
            HapticConstantInfo("Clock Tick", "CLOCK_TICK", HapticFeedbackConstants.CLOCK_TICK, 21),
            HapticConstantInfo("Context Click", "CONTEXT_CLICK", HapticFeedbackConstants.CONTEXT_CLICK, 23),
            HapticConstantInfo("Gesture Start", "GESTURE_START", 12, 30),
            HapticConstantInfo("Gesture End", "GESTURE_END", 13, 30),
            HapticConstantInfo("Gesture Threshold Activate", "GESTURE_THRESHOLD_ACTIVATE", 23, 34),
            HapticConstantInfo("Gesture Threshold Deactivate", "GESTURE_THRESHOLD_DEACTIVATE", 24, 34),
            HapticConstantInfo("Text Handle Move", "TEXT_HANDLE_MOVE", HapticFeedbackConstants.TEXT_HANDLE_MOVE, 27),
            HapticConstantInfo("Virtual Key", "VIRTUAL_KEY", HapticFeedbackConstants.VIRTUAL_KEY, 3),
            HapticConstantInfo("Virtual Key Release", "VIRTUAL_KEY_RELEASE", HapticFeedbackConstants.VIRTUAL_KEY_RELEASE, 27),
            HapticConstantInfo("Drag Start", "DRAG_START", 25, 34),
            HapticConstantInfo("Segment Tick", "SEGMENT_TICK", 26, 34),
            HapticConstantInfo("Segment Frequent Tick", "SEGMENT_FREQUENT_TICK", 27, 34),
        )

        val allEffects = listOf(
            EffectInfo("Click", "EFFECT_CLICK", VibrationEffect.EFFECT_CLICK),
            EffectInfo("Double Click", "EFFECT_DOUBLE_CLICK", VibrationEffect.EFFECT_DOUBLE_CLICK),
            EffectInfo("Tick", "EFFECT_TICK", VibrationEffect.EFFECT_TICK),
            EffectInfo("Heavy Click", "EFFECT_HEAVY_CLICK", VibrationEffect.EFFECT_HEAVY_CLICK),
        )

        val allPrimitives = listOf(
            PrimitiveInfo("Click", "PRIMITIVE_CLICK", VibrationEffect.Composition.PRIMITIVE_CLICK),
            PrimitiveInfo("Tick", "PRIMITIVE_TICK", VibrationEffect.Composition.PRIMITIVE_TICK),
            PrimitiveInfo("Low Tick", "PRIMITIVE_LOW_TICK", VibrationEffect.Composition.PRIMITIVE_LOW_TICK),
            PrimitiveInfo("Quick Rise", "PRIMITIVE_QUICK_RISE", VibrationEffect.Composition.PRIMITIVE_QUICK_RISE),
            PrimitiveInfo("Slow Rise", "PRIMITIVE_SLOW_RISE", VibrationEffect.Composition.PRIMITIVE_SLOW_RISE),
            PrimitiveInfo("Quick Fall", "PRIMITIVE_QUICK_FALL", VibrationEffect.Composition.PRIMITIVE_QUICK_FALL),
            PrimitiveInfo("Spin", "PRIMITIVE_SPIN", VibrationEffect.Composition.PRIMITIVE_SPIN),
            PrimitiveInfo("Thud", "PRIMITIVE_THUD", VibrationEffect.Composition.PRIMITIVE_THUD),
        )
    }
}
