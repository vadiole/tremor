package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.R
import kotlin.math.abs
import kotlin.math.roundToInt

class DrumRollerView(
    context: Context,
    private val minValue: Float = 0f,
    private val maxValue: Float = 1f,
    private val step: Float = 0.05f,
) : View(context) {

    var value: Float = maxValue
        private set

    var onValueChanged: ((Float) -> Unit)? = null

    private val density = resources.displayMetrics.density

    private val drumWidth = (20 * density).toInt()
    private val drumHeight = (48 * density).toInt()
    private val lineSpacing = 8f * density
    private val borderRadius = 4f * density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        strokeWidth = 1f * density
        style = Paint.Style.STROKE
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private val totalSteps = ((maxValue - minValue) / step).roundToInt()
    private var scrollOffset = 0f
    private var lastTouchY = 0f
    private var currentStep = totalSteps

    init {
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(drumWidth, drumHeight)
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, borderRadius, borderRadius, bgPaint)
        canvas.drawRoundRect(rect, borderRadius, borderRadius, borderPaint)

        canvas.save()
        canvas.clipRect(rect)

        val centerY = height / 2f
        val offsetInPx = scrollOffset * lineSpacing

        var lineY = centerY + offsetInPx
        while (lineY > -lineSpacing) {
            lineY -= lineSpacing
        }

        while (lineY < height + lineSpacing) {
            val distFromCenter = abs(lineY - centerY)
            val halfHeight = height / 2f
            val alpha = if (distFromCenter < halfHeight) {
                val t = distFromCenter / halfHeight
                val fade = 1f - t * t
                (fade * 255).toInt().coerceIn(0, 255)
            } else {
                0
            }
            linePaint.alpha = alpha
            canvas.drawLine(4f * density, lineY, width - 4f * density, lineY, linePaint)
            lineY += lineSpacing
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - lastTouchY
                scrollOffset += dy / lineSpacing
                lastTouchY = event.y

                while (scrollOffset >= 1f && currentStep > 0) {
                    scrollOffset -= 1f
                    currentStep--
                    updateValue()
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                while (scrollOffset <= -1f && currentStep < totalSteps) {
                    scrollOffset += 1f
                    currentStep++
                    updateValue()
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }

                if (currentStep == 0 && scrollOffset > 0f) scrollOffset = 0f
                if (currentStep == totalSteps && scrollOffset < 0f) scrollOffset = 0f

                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                scrollOffset = 0f
                invalidate()
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun updateValue() {
        val newValue = minValue + currentStep * step
        value = newValue.coerceIn(minValue, maxValue)
        onValueChanged?.invoke(value)
    }

    fun setValue(newValue: Float) {
        currentStep = ((newValue - minValue) / step).roundToInt().coerceIn(0, totalSteps)
        value = minValue + currentStep * step
        scrollOffset = 0f
        invalidate()
    }
}
