package vadiole.tremor.view

import android.content.Context
import android.content.res.Configuration
import kotlin.math.abs
import kotlin.math.ceil

object Floating {
    const val SHADOW_RADIUS_DP = 6f
    const val SHADOW_DY_DP = 2f
    const val BORDER_WIDTH_DP = 0.11f

    private const val NIGHT_BORDER_WIDTH_DP = 1f

    fun borderWidthPx(context: Context): Float {
        val widthDp = if (context.resources.configuration.isNightMode) {
            NIGHT_BORDER_WIDTH_DP
        } else {
            BORDER_WIDTH_DP
        }
        return (widthDp * context.density).coerceAtLeast(1f)
    }

    /** Half the border stroke — the inset at which a surface's border is centred on its bounds. */
    fun surfaceInsetPx(context: Context): Float = borderWidthPx(context) / 2f

    fun shadowRadiusPx(context: Context): Float = SHADOW_RADIUS_DP * context.density
    fun shadowDyPx(context: Context): Float = SHADOW_DY_DP * context.density

    // setShadowLayer blur is Gaussian; budget ~3σ of the tail so it isn't clipped. σ = 0.57735·radius + 0.5 (Skia SkBlurMask::ConvertRadiusToSigma)
    fun shadowOutsetPx(context: Context): Int =
        ceil(3f * (0.57735f * shadowRadiusPx(context) + 0.5f) + abs(shadowDyPx(context))).toInt()

    private val Context.density: Float get() = resources.displayMetrics.density

    private val Configuration.isNightMode: Boolean
        get() = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
