package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.View
import vadiole.tremor.R

class HapticToggle(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private val trackWidth = (52 * density).toInt()
    private val trackHeight = (28 * density).toInt()
    private val thumbRadius = 10f * density
    private val thumbPadding = 4f * density

    private var isOn = false

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val trackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        style = Paint.Style.FILL
    }

    private val onColor = context.getColor(R.color.foreground)
    private val offColor = context.getColor(R.color.surface)
    private val thumbOnColor = context.getColor(R.color.background)
    private val thumbOffColor = context.getColor(R.color.foreground)

    private val rect = RectF()

    init {
        isClickable = true
        isFocusable = true
        setOnClickListener {
            isOn = !isOn
            if (isOn) {
                performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                performHapticFeedback(HapticFeedbackConstants.REJECT)
            }
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(trackWidth, trackHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val cornerRadius = height / 2f
        val halfStroke = trackBorderPaint.strokeWidth / 2f

        trackPaint.color = if (isOn) onColor else offColor
        thumbPaint.color = if (isOn) thumbOnColor else thumbOffColor

        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, trackPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, trackBorderPaint)

        val centerY = height / 2f
        val thumbX = if (isOn) {
            width - thumbPadding - thumbRadius - halfStroke
        } else {
            thumbPadding + thumbRadius + halfStroke
        }
        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbPaint)
    }
}
