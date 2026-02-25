package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import vadiole.tremor.Density
import vadiole.tremor.R
import kotlin.math.abs

class ScrollWheelView(context: Context) : View(context), Density {

    private val viewHeight = 56.dp()
    private val cornerRadius = 6f.dp()
    private val tickSpacing = 12f.dp()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        strokeWidth = 1f.dp()
        style = Paint.Style.STROKE
    }

    private val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        strokeWidth = 1f.dp()
        style = Paint.Style.STROKE
    }

    private val streakThreshold = 2f.dp()

    private val rect = RectF()
    private var scrollOffset = 0f
    private var lastTouchX = 0f
    private var velocity = 0f
    private var lastMoveTime = 0L
    private var isFlung = false
    private val friction = 0.92f
    private val minVelocity = 0.5f.dp()

    private val flingRunnable = object : Runnable {
        override fun run() {
            if (!isFlung) return
            velocity *= friction
            scrollOffset += velocity
            checkTick()
            invalidate()
            if (abs(velocity) > minVelocity) {
                postOnAnimation(this)
            } else {
                isFlung = false
            }
        }
    }

    init {
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        canvas.save()
        canvas.clipRect(rect)

        val centerX = width / 2f
        val offsetPx = scrollOffset % tickSpacing

        var x = offsetPx
        while (x < 0) x += tickSpacing
        x -= tickSpacing

        val topY = 8f.dp()
        val bottomY = height - 8f.dp()
        val halfWidth = width / 2f

        val absVelocity = abs(velocity)
        val streakLength = if (absVelocity > streakThreshold) {
            (absVelocity * 0.3f).coerceAtMost(8f.dp())
        } else 0f
        val streakDir = if (velocity > 0) -1f else 1f

        while (x < width + tickSpacing) {
            val distFromCenter = abs(x - centerX)
            val alpha = if (distFromCenter < halfWidth) {
                val t = distFromCenter / halfWidth
                val fade = 1f - t * t
                (fade * 255).toInt().coerceIn(0, 255)
            } else {
                0
            }
            tickPaint.alpha = alpha
            canvas.drawLine(x, topY, x, bottomY, tickPaint)

            if (streakLength > 0f) {
                streakPaint.alpha = (alpha * 0.4f).toInt().coerceIn(0, 255)
                val streakEnd = x + streakLength * streakDir
                canvas.drawLine(streakEnd, topY, streakEnd, bottomY, streakPaint)
            }

            x += tickSpacing
        }

        canvas.restore()
    }

    private var lastTickOffset = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isFlung = false
                removeCallbacks(flingRunnable)
                lastTouchX = event.x
                lastMoveTime = System.currentTimeMillis()
                velocity = 0f
                lastTickOffset = scrollOffset
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val now = System.currentTimeMillis()
                val dt = (now - lastMoveTime).coerceAtLeast(1)
                velocity = dx / dt * 16f
                lastMoveTime = now
                lastTouchX = event.x
                scrollOffset += dx
                checkTick()
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (abs(velocity) > minVelocity) {
                    isFlung = true
                    postOnAnimation(flingRunnable)
                }
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                isFlung = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun checkTick() {
        val delta = scrollOffset - lastTickOffset
        if (abs(delta) >= tickSpacing) {
            val ticks = (delta / tickSpacing).toInt()
            lastTickOffset += ticks * tickSpacing
            performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(flingRunnable)
    }
}
