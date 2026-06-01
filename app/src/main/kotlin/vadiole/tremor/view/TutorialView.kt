package vadiole.tremor.view

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.provider.Settings
import android.view.Gravity
import android.view.WindowInsets
import android.widget.TextView
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.UiConstants

class TutorialView(context: Context) : TextView(context), Density {

    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(
        context,
        UiConstants.CORNER_RADIUS_DP.dp.toInt(),
    )

    init {
        text = context.getText(R.string.haptic_disabled_message)
        setTextColor(context.getColor(R.color.foreground))
        textSize = 12f
        typeface = Typeface.MONOSPACE
        gravity = Gravity.CENTER
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
        setPadding(16.dp, 12.dp, 16.dp, 12.dp)
        setOnApplyWindowInsetsListener { v, insets ->
            val navBar = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()).bottom
            v.setPadding(16.dp, 12.dp, 16.dp, 12.dp + navBar)
            insets
        }
        setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
        }
        visibility = GONE
    }

    override fun onDetachedFromWindow() {
        surfaceDrawable.cancelAnimations()
        super.onDetachedFromWindow()
    }
}
