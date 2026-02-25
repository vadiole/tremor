package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.R

class HapticButton(
    context: Context,
    private val label: String,
    private val constantName: String,
    private val onTrigger: (screenX: Float, screenY: Float) -> Unit,
) : View(context) {

    private val density = resources.displayMetrics.density

    private val cornerRadius = 6f * density
    private val minHeight = (48 * density).toInt()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 13f * density
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    private val constantPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = 9f * density
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    private val pressedColor = context.getColor(R.color.surface_pressed)
    private val normalColor = context.getColor(R.color.surface)

    private val rect = RectF()
    private val location = IntArray(2)

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, minHeight)
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val centerX = width / 2f
        val labelY = height / 2f - 2f * density
        val constantY = labelY + 12f * density

        canvas.drawText(label, centerX, labelY, labelPaint)
        canvas.drawText(constantName, centerX, constantY, constantPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                bgPaint.color = pressedColor
                invalidate()
                getLocationOnScreen(location)
                val screenX = location[0] + event.x
                val screenY = location[1] + event.y
                onTrigger(screenX, screenY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                bgPaint.color = normalColor
                invalidate()
            }
        }
        return true
    }
}
