package vadiole.tremor.view

import android.content.Context
import android.content.res.Configuration

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
        return (widthDp * context.resources.displayMetrics.density).coerceAtLeast(1f)
    }

    private val Configuration.isNightMode: Boolean
        get() = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
