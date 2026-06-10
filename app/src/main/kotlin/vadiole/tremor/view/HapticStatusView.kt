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
            val primary = if (opensBatterySaverSettings) {
                Settings.ACTION_BATTERY_SAVER_SETTINGS
            } else {
                Settings.ACTION_SOUND_SETTINGS
            }
            if (!openSettings(primary)) openSettings(Settings.ACTION_SETTINGS)
        }
        visibility = GONE
    }

    fun bind(systemHapticsOff: Boolean, powerSaveOn: Boolean) {
        opensBatterySaverSettings = !systemHapticsOff && powerSaveOn
        val content = SpannableStringBuilder()
        if (systemHapticsOff) {
            content.append(context.getText(R.string.haptics_off_title))
            content.append("\n")
            appendDetail(content, fixLineRes())
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

    private fun fixLineRes(): Int {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer == "samsung" -> R.string.haptics_off_fix_samsung
            manufacturer == "google" && Build.VERSION.SDK_INT >= 33 -> R.string.haptics_off_fix_pixel
            manufacturer == "google" -> R.string.haptics_off_fix_pixel_12
            else -> R.string.haptics_off_fix_generic
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
