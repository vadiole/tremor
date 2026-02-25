package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R
import kotlin.math.abs
import kotlin.math.roundToInt

class DrumRollerView(
    context: Context,
    private val minValue: Float = 0f,
    private val maxValue: Float = 1f,
    private val step: Float = 0.05f,
) : View(context), Density {

    var value: Float = maxValue
        private set

    var onValueChanged: ((Float) -> Unit)? = null

    private val visualWidth = 20.dp()
    private val visualHeight = 48.dp()
    private val touchWidth = 48.dp()
    private val touchHeight = 64.dp()
    private val lineSpacing = 8f.dp()
    private val borderRadius = 4f.dp()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        strokeWidth = 1f.dp()
        style = Paint.Style.STROKE
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private val totalSteps = ((maxValue - minValue) / step).roundToInt()
    private var scrollOffset = 0f
    private var lastTouchY = 0f
    private var lastMoveTime = 0L
    private var velocity = 0f
    private var isFlung = false
    private val friction = 0.90f
    private val minVelocity = 0.3f
    private val maxVelocity = 15f
    private var currentStep = totalSteps

    private val flingRunnable = object : Runnable {
        override fun run() {
            if (!isFlung) return
            velocity *= friction
            scrollOffset += velocity

            while (scrollOffset >= 1f && currentStep > 0) {
                scrollOffset -= 1f
                currentStep--
                updateValue()
                performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
            }
            while (scrollOffset <= -1f && currentStep < totalSteps) {
                scrollOffset += 1f
                currentStep++
                updateValue()
                performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
            }

            if (currentStep == 0 && scrollOffset > 0f) scrollOffset = 0f
            if (currentStep == totalSteps && scrollOffset < 0f) scrollOffset = 0f

            invalidate()
            if (abs(velocity) > minVelocity) {
                postOnAnimation(this)
            } else {
                isFlung = false
                scrollOffset = 0f
                invalidate()
            }
        }
    }

    init {
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(touchWidth, touchHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val offsetX = (width - visualWidth) / 2f
        val offsetY = (height - visualHeight) / 2f

        rect.set(offsetX, offsetY, offsetX + visualWidth, offsetY + visualHeight)
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

        val drumLeft = offsetX + 4f.dp()
        val drumRight = offsetX + visualWidth - 4f.dp()
        val halfVisual = visualHeight / 2f

        while (lineY < height + lineSpacing) {
            val distFromCenter = abs(lineY - centerY)
            val alpha = if (distFromCenter < halfVisual) {
                val t = distFromCenter / halfVisual
                val fade = 1f - t * t
                (fade * 255).toInt().coerceIn(0, 255)
            } else {
                0
            }
            linePaint.alpha = alpha
            canvas.drawLine(drumLeft, lineY, drumRight, lineY, linePaint)
            lineY += lineSpacing
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isFlung = false
                removeCallbacks(flingRunnable)
                lastTouchY = event.y
                lastMoveTime = SystemClock.uptimeMillis()
                velocity = 0f
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - lastTouchY
                val now = SystemClock.uptimeMillis()
                val dt = (now - lastMoveTime).coerceAtLeast(1)
                velocity = ((dy / lineSpacing) / dt * 16f).coerceIn(-maxVelocity, maxVelocity)
                lastMoveTime = now
                lastTouchY = event.y

                scrollOffset += dy / lineSpacing

                while (scrollOffset >= 1f && currentStep > 0) {
                    scrollOffset -= 1f
                    currentStep--
                    updateValue()
                    performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                }
                while (scrollOffset <= -1f && currentStep < totalSteps) {
                    scrollOffset += 1f
                    currentStep++
                    updateValue()
                    performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                }

                if (currentStep == 0 && scrollOffset > 0f) scrollOffset = 0f
                if (currentStep == totalSteps && scrollOffset < 0f) scrollOffset = 0f

                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (abs(velocity) > minVelocity) {
                    isFlung = true
                    postOnAnimation(flingRunnable)
                } else {
                    scrollOffset = 0f
                    invalidate()
                }
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                isFlung = false
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(flingRunnable)
    }

}
