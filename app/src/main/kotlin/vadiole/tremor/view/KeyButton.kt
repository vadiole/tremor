package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.TouchEffect
import vadiole.tremor.UiConstants

class KeyButton(
    context: Context,
    private val letter: Char,
) : View(context), Density {

    private val surfaceDrawable = FloatingSurfaceDrawable(
        context = context,
        pathProvider = FloatingSurfaceDrawable.squircle(UiConstants.CORNER_RADIUS_DP.dp.toInt()),
    )

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 16f.sp
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val letterStr = letter.toString()

    init {
        isClickable = true
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
        setOnTouchListener(
            TouchEffect(
                pressedScale = 1.08f,
                rubberBandDrag = true,
                maxDragPx = 5f.dp,
            ),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val textY = cy - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(letterStr, cx, textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                if (event.action == MotionEvent.ACTION_UP) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
                }
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        surfaceDrawable.cancelAnimations()
        super.onDetachedFromWindow()
    }
}
