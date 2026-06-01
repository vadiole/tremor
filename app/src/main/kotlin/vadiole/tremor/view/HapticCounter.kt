package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.TouchEffect
import vadiole.tremor.UiConstants

class HapticCounter(context: Context) : View(context), Density {

    private val viewHeight = 56.dp
    private val cornerRadius = UiConstants.CORNER_RADIUS_DP.dp

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var count = prefs.getInt(KEY_COUNT, 0)

    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(context, cornerRadius.toInt())
    private val surfaceInset = Floating.surfaceInsetPx(context)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 16f.sp
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 20f.sp
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        strokeWidth = 1f.dp
    }

    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface_pressed)
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private val pressedRect = RectF()
    private val clipPath = Path()
    private var pressedZone = 0 // -1 = minus, 1 = plus, 0 = none

    init {
        isClickable = true
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
        setOnTouchListener(
            TouchEffect(
                pressedScale = 1.02f,
                rubberBandDrag = true,
                maxDragPx = 5f.dp,
                dragDamping = 0.06f,
            ) { view, event ->
                val thirdW = view.width / 3f
                event.x < thirdW || event.x > thirdW * 2
            },
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = surfaceInset
        rect.set(0f, 0f, width.toFloat(), height.toFloat())

        if (pressedZone != 0) {
            val thirdW = width / 3f
            canvas.save()
            if (surfaceDrawable.copySurfacePath(clipPath)) {
                canvas.clipPath(clipPath)
            }
            if (pressedZone == -1) {
                pressedRect.set(halfStroke, halfStroke, thirdW, height - halfStroke)
            } else {
                pressedRect.set(thirdW * 2, halfStroke, width - halfStroke, height - halfStroke)
            }
            canvas.drawRect(pressedRect, pressedPaint)
            canvas.restore()
        }

        val thirdW = width / 3f
        val centerY = height / 2f - (buttonPaint.ascent() + buttonPaint.descent()) / 2f
        val textCenterY = height / 2f - (textPaint.ascent() + textPaint.descent()) / 2f

        canvas.drawLine(thirdW, halfStroke, thirdW, height - halfStroke, dividerPaint)
        canvas.drawLine(thirdW * 2, halfStroke, thirdW * 2, height - halfStroke, dividerPaint)

        canvas.drawText("\u2212", thirdW / 2f, centerY, buttonPaint)
        canvas.drawText(count.toString(), width / 2f, textCenterY, textPaint)
        canvas.drawText("+", thirdW * 2 + thirdW / 2f, centerY, buttonPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val thirdW = width / 3f
                val zone = if (event.x < thirdW) -1 else if (event.x > thirdW * 2) 1 else 0
                if (zone != 0) {
                    count += zone
                    prefs.edit().putInt(KEY_COUNT, count).apply()
                    pressedZone = zone
                    isPressed = true
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pressedZone = 0
                isPressed = false
                invalidate()
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        surfaceDrawable.cancelAnimations()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val PREFS = "tremor"
        const val KEY_COUNT = "counter"
    }
}
