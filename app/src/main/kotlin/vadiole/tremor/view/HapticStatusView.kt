package vadiole.tremor.view

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.WindowInsets
import android.widget.TextView
import vadiole.tremor.Density
import vadiole.tremor.HapticEngine.HapticsOffCause
import vadiole.tremor.R
import vadiole.tremor.UiConstants

class HapticStatusView(context: Context) : TextView(context), Density {

    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(
        context,
        UiConstants.CORNER_RADIUS_DP.dp.toInt(),
    )

    private val detailColor = context.getColor(R.color.text_secondary)
    private var opensBatterySaverSettings = false
    private var pulsing = false

    init {
        setTextColor(context.getColor(R.color.foreground))
        textSize = 12f
        typeface = Typeface.MONOSPACE
        gravity = Gravity.CENTER
        setLineSpacing(0f, 1.25f)
        accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
        setPadding(16.dp, 12.dp, 16.dp, 12.dp)
        setOnApplyWindowInsetsListener { v, insets ->
            val navBar = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()).bottom
            v.setPadding(16.dp, 12.dp, 16.dp, 12.dp + navBar)
            insets
        }
        setOnClickListener {
            if (!openSettings(primarySettingsAction())) openSettings(Settings.ACTION_SETTINGS)
        }
        visibility = GONE
    }

    fun bind(offCause: HapticsOffCause?, powerSaveOn: Boolean) {
        opensBatterySaverSettings = offCause == null && powerSaveOn
        val content = SpannableStringBuilder()
        if (offCause != null) {
            content.append(context.getText(titleRes(offCause)))
            content.append("\n")
            appendDetail(content, fixLineRes(offCause))
        }
        if (powerSaveOn) {
            if (content.isEmpty()) {
                content.append(context.getText(R.string.battery_saver_muting))
            } else {
                content.append("\n")
                appendDetail(content, R.string.battery_saver_muting)
            }
        }
        text = content
    }

    fun show() {
        animate().cancel()
        alpha = 1f
        translationY = 0f
        scaleX = 1f
        scaleY = 1f
        pulsing = false
        visibility = VISIBLE
    }

    fun hide() {
        animate().cancel()
        pulsing = false
        visibility = GONE
    }

    fun dismissAnimated() {
        if (visibility != VISIBLE) return
        animate().cancel()
        pulsing = false
        animate().alpha(0f).translationY(12f.dp).setDuration(DISMISS_MS).withEndAction {
            visibility = GONE
            alpha = 1f
            translationY = 0f
        }.start()
    }

    fun pulse() {
        if (pulsing || visibility != VISIBLE) return
        pulsing = true
        animate().scaleX(PULSE_SCALE_X).scaleY(PULSE_SCALE_Y).setDuration(PULSE_UP_MS).withEndAction {
            animate().scaleX(1f).scaleY(1f).setDuration(PULSE_DOWN_MS).withEndAction { pulsing = false }.start()
        }.start()
    }

    private fun appendDetail(builder: SpannableStringBuilder, resId: Int) {
        val start = builder.length
        builder.append(context.getText(resId))
        builder.setSpan(RelativeSizeSpan(DETAIL_TEXT_SCALE), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(detailColor), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // Settings-path families. AOSP (stock Pixel / near-stock Motorola) reuses the framework
    // labels verbatim; the rest are top-5 global makers with their own skins (One UI, HyperOS,
    // ColorOS/OxygenOS, Funtouch/OriginOS, HiOS/XOS).
    private enum class Profile { AOSP, SAMSUNG, XIAOMI, OPPO, VIVO, TRANSSION, GENERIC }

    private fun deviceProfile(): Profile {
        val m = Build.MANUFACTURER.lowercase()
        return when {
            m == "samsung" -> Profile.SAMSUNG
            m == "google" || m == "motorola" -> Profile.AOSP
            m == "xiaomi" -> Profile.XIAOMI
            m == "oppo" || m == "oneplus" || m == "realme" -> Profile.OPPO
            m == "vivo" -> Profile.VIVO
            "tecno" in m || "infinix" in m || "itel" in m -> Profile.TRANSSION
            else -> Profile.GENERIC
        }
    }

    // The master vs touch cause is only trustworthy on stock Android, so only it gets the
    // touch-specific title; every other family shows the neutral "haptics are off" line.
    private fun titleRes(cause: HapticsOffCause): Int {
        return if (deviceProfile() == Profile.AOSP && cause == HapticsOffCause.TOUCH) {
            R.string.haptics_off_title_touch
        } else {
            R.string.haptics_off_title
        }
    }

    private fun fixLineRes(cause: HapticsOffCause): Int {
        return when (deviceProfile()) {
            Profile.AOSP -> when {
                Build.VERSION.SDK_INT < 33 -> R.string.haptics_off_fix_aosp_12
                cause == HapticsOffCause.TOUCH -> R.string.haptics_off_fix_aosp_touch
                else -> R.string.haptics_off_fix_aosp
            }

            Profile.SAMSUNG -> R.string.haptics_off_fix_samsung
            Profile.XIAOMI -> if (Build.VERSION.SDK_INT >= 34) {
                R.string.haptics_off_fix_xiaomi
            } else {
                R.string.haptics_off_fix_xiaomi_miui
            }

            Profile.OPPO -> R.string.haptics_off_fix_oppo
            Profile.VIVO -> R.string.haptics_off_fix_vivo
            Profile.TRANSSION -> R.string.haptics_off_fix_transsion
            Profile.GENERIC -> R.string.haptics_off_fix_generic
        }
    }

    private fun primarySettingsAction(): String {
        if (opensBatterySaverSettings) return Settings.ACTION_BATTERY_SAVER_SETTINGS
        return when (deviceProfile()) {
            // Stock Android 12/12L hosts the vibration screen under Accessibility, not Sound.
            Profile.AOSP -> if (Build.VERSION.SDK_INT < 33) {
                Settings.ACTION_ACCESSIBILITY_SETTINGS
            } else {
                Settings.ACTION_SOUND_SETTINGS
            }
            Profile.TRANSSION -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            Profile.GENERIC -> Settings.ACTION_SETTINGS
            else -> Settings.ACTION_SOUND_SETTINGS
        }
    }

    private fun openSettings(action: String): Boolean {
        return try {
            val intent = Intent(action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onDetachedFromWindow() {
        animate().cancel()
        surfaceDrawable.cancelAnimations()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val DETAIL_TEXT_SCALE = 0.92f
        const val DISMISS_MS = 220L
        const val PULSE_UP_MS = 90L
        const val PULSE_DOWN_MS = 160L
        const val PULSE_SCALE_X = 1.02f
        const val PULSE_SCALE_Y = 1.05f
    }
}
