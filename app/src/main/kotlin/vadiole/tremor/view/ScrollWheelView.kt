package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.UiConstants
import kotlin.math.abs

class ScrollWheelView(context: Context) : View(context), Density {

    private val viewHeight = 56.dp
    private val cornerRadius = UiConstants.CORNER_RADIUS_DP.dp
    private val tickSpacing = 12f.dp
    private val tickHapticConstant = HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(context, cornerRadius.toInt())
    private val surfaceInset = Floating.surfaceInsetPx(context)

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        strokeWidth = 1f.dp
        style = Paint.Style.STROKE
    }

    private val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        strokeWidth = 1f.dp
        style = Paint.Style.STROKE
    }

    private val streakThreshold = 2f.dp

    private val rect = RectF()
    private var scrollOffset = 0f
    private var lastTouchX = 0f
    private var velocity = 0f
    private var lastMoveTime = 0L
    private var isFlung = false
    private val friction = 0.96f
    private val minVelocity = 0.2f.dp
    private val maxVelocity = 50f.dp

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var claimed = false

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
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(surfaceInset, surfaceInset, width - surfaceInset, height - surfaceInset)

        canvas.save()
        canvas.clipRect(rect)

        val centerX = width / 2f
        val offsetPx = scrollOffset % tickSpacing

        var x = offsetPx
        while (x < 0) x += tickSpacing
        x -= tickSpacing

        val topY = 8f.dp
        val bottomY = height - 8f.dp
        val halfWidth = width / 2f

        val absVelocity = abs(velocity)
        val streakLength = if (absVelocity > streakThreshold) {
            (absVelocity * 0.3f).coerceAtMost(8f.dp)
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
                downX = event.x
                downY = event.y
                claimed = false
                lastTouchX = event.x
                lastMoveTime = SystemClock.uptimeMillis()
                velocity = 0f
                lastTickOffset = scrollOffset
                isPressed = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!claimed) {
                    val dx = abs(event.x - downX)
                    val dy = abs(event.y - downY)
                    if (dx > touchSlop && dx > dy) {
                        claimed = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                        lastTouchX = event.x
                        lastMoveTime = SystemClock.uptimeMillis()
                    }
                } else {
                    val dx = event.x - lastTouchX
                    val now = SystemClock.uptimeMillis()
                    val dt = (now - lastMoveTime).coerceAtLeast(1)
                    velocity = (dx / dt * 16f).coerceIn(-maxVelocity, maxVelocity)
                    lastMoveTime = now
                    lastTouchX = event.x
                    scrollOffset += dx
                    checkTick()
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (claimed && abs(velocity) > minVelocity) {
                    isFlung = true
                    postOnAnimation(flingRunnable)
                }
                isPressed = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                isFlung = false
                isPressed = false
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
            performHapticFeedback(tickHapticConstant)
        }
    }

    override fun onDetachedFromWindow() {
        surfaceDrawable.cancelAnimations()
        super.onDetachedFromWindow()
        removeCallbacks(flingRunnable)
    }
}
