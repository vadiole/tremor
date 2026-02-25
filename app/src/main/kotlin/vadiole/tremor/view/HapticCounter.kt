package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R

class HapticCounter(context: Context) : View(context), Density {

    private val viewHeight = 56.dp()
    private val cornerRadius = 6f.dp()

    private val prefs = context.getSharedPreferences("tremor", Context.MODE_PRIVATE)
    private var count = prefs.getInt("counter", 0)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 16f.dp()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 20f.dp()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        strokeWidth = 1f.dp()
    }

    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface_pressed)
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private val pressedRect = RectF()
    private var pressedZone = 0 // -1 = minus, 1 = plus, 0 = none

    init {
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        if (pressedZone != 0) {
            val thirdW = width / 3f
            canvas.save()
            val clipPath = android.graphics.Path()
            clipPath.addRoundRect(rect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
            canvas.clipPath(clipPath)
            if (pressedZone == -1) {
                pressedRect.set(halfStroke, halfStroke, thirdW, height - halfStroke)
            } else {
                pressedRect.set(thirdW * 2, halfStroke, width - halfStroke, height - halfStroke)
            }
            canvas.drawRect(pressedRect, pressedPaint)
            canvas.restore()
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val thirdW = width / 3f
        val centerY = height / 2f + buttonPaint.textSize / 3f
        val textCenterY = height / 2f + textPaint.textSize / 3f

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
                when {
                    event.x < thirdW -> {
                        count--
                        prefs.edit().putInt("counter", count).apply()
                        pressedZone = -1
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
                        invalidate()
                    }
                    event.x > thirdW * 2 -> {
                        count++
                        prefs.edit().putInt("counter", count).apply()
                        pressedZone = 1
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (pressedZone != 0) {
                    animate().scaleX(1f).scaleY(1f).setDuration(150)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2f)).start()
                }
                pressedZone = 0
                invalidate()
            }
        }
        return true
    }
}
