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

    private var count = 0

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
    }

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 20f.dp()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        strokeWidth = 1f.dp()
    }

    private val rect = RectF()

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
        if (event.action == MotionEvent.ACTION_DOWN) {
            val thirdW = width / 3f
            when {
                event.x < thirdW -> {
                    count--
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    invalidate()
                }
                event.x > thirdW * 2 -> {
                    count++
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    invalidate()
                }
            }
        }
        return true
    }
}
