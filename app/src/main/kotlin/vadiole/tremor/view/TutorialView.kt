package vadiole.tremor.view

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.provider.Settings
import android.view.Gravity
import android.view.WindowInsets
import android.widget.TextView
import vadiole.tremor.Density
import vadiole.tremor.R

class TutorialView(context: Context) : TextView(context), Density {

    private val borderPaint = Paint().apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.FILL
    }
    private val borderHeight = 1f.dp

    init {
        text = context.getString(R.string.haptic_disabled_message)
        setTextColor(context.getColor(R.color.foreground))
        textSize = 12f
        typeface = Typeface.MONOSPACE
        gravity = Gravity.CENTER
        setBackgroundColor(context.getColor(R.color.surface))
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), borderHeight, borderPaint)
    }
}
