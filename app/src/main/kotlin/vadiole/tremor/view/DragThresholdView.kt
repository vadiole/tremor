package vadiole.tremor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import vadiole.tremor.Density
import vadiole.tremor.R

class DragThresholdView(context: Context) : View(context), Density {

    private val viewHeight = 72.dp()
    private val cornerRadius = 6f.dp()
    private val handleWidth = 36f.dp()
    private val handlePadding = 6f.dp()
    private val handleCornerRadius = 8f.dp()
    private val thresholdFraction = 0.75f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val thresholdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_muted)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f.dp()
        pathEffect = DashPathEffect(floatArrayOf(4f.dp(), 4f.dp()), 0f)
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface_pressed)
        style = Paint.Style.FILL
    }

    private val handleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_muted)
        strokeWidth = 1.5f.dp()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val rect = RectF()
    private val handleRect = RectF()
    private val clipPath = Path()

    private var handleX = 0f
    private var isDragging = false
    private var dragStartX = 0f
    private var handleStartX = 0f
    private var activated = false
    private var gestureStarted = false

    // handle scale animation on threshold crossing
    private var handleScale = 1f
    private var scaleAnimator: ValueAnimator? = null

    // spring return animation
    private var springVelocity = 0f
    private var isAnimating = false
    private var lastSpringTime = 0L
    private val springStiffness = 560f
    private val springDamping = 25f

    private val springRunnable = object : Runnable {
        override fun run() {
            if (!isAnimating) return

            val now = System.nanoTime()
            val dt = ((now - lastSpringTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastSpringTime = now

            val restX = handlePadding
            val dx = handleX - restX
            val accel = -springStiffness * dx - springDamping * springVelocity

            springVelocity += accel * dt
            handleX += springVelocity * dt

            val energy = dx * dx + springVelocity * springVelocity

            if (energy < 0.5f) {
                handleX = restX
                isAnimating = false
            } else {
                postOnAnimation(this)
            }
            invalidate()
        }
    }

    init {
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, viewHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        handleX = handlePadding
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // threshold line
        val thresholdX = width * thresholdFraction
        val lineTop = 12f.dp()
        val lineBottom = height - 12f.dp()
        canvas.drawLine(thresholdX, lineTop, thresholdX, lineBottom, thresholdPaint)

        // handle
        val handleLeft = handleX
        val handleTop = handlePadding
        val handleBottom = height.toFloat() - handlePadding
        handleRect.set(handleLeft, handleTop, handleLeft + handleWidth, handleBottom)

        // clip to container bounds
        canvas.save()
        clipPath.reset()
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(clipPath)

        // apply scale around handle center
        if (handleScale != 1f) {
            canvas.scale(handleScale, handleScale, handleRect.centerX(), handleRect.centerY())
        }

        // fully rounded corners
        canvas.drawRoundRect(handleRect, handleCornerRadius, handleCornerRadius, handlePaint)
        canvas.drawRoundRect(handleRect, handleCornerRadius, handleCornerRadius, handleBorderPaint)

        // chevron (right-pointing >)
        val chevronCx = handleRect.centerX() + 2f.dp()
        val chevronCy = handleRect.centerY()
        val chevronSize = 5f.dp()
        canvas.drawLine(chevronCx - chevronSize, chevronCy - chevronSize, chevronCx, chevronCy, chevronPaint)
        canvas.drawLine(chevronCx, chevronCy, chevronCx - chevronSize, chevronCy + chevronSize, chevronPaint)

        canvas.restore()
    }

    private fun animateScaleTo(targetScale: Float) {
        scaleAnimator?.cancel()
        scaleAnimator = ValueAnimator.ofFloat(handleScale, targetScale).apply {
            duration = 250
            interpolator = OvershootInterpolator(3f)
            addUpdateListener {
                handleScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchRight = handleX + handleWidth + 16f.dp()
                if (event.x <= touchRight) {
                    isDragging = true
                    isAnimating = false
                    removeCallbacks(springRunnable)
                    dragStartX = event.x
                    handleStartX = handleX
                    gestureStarted = false
                    activated = false
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) return true

                if (!gestureStarted) {
                    gestureStarted = true
                    val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.DRAG_START
                            else HapticFeedbackConstants.GESTURE_START
                    performHapticFeedback(c)
                }

                val dx = event.x - dragStartX
                handleX = (handleStartX + dx).coerceIn(handlePadding, width - handleWidth - handlePadding)

                val thresholdX = width * thresholdFraction
                val handleCenter = handleX + handleWidth / 2f
                val wasActivated = activated
                activated = handleCenter >= thresholdX

                if (activated && !wasActivated) {
                    val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
                            else HapticFeedbackConstants.CONFIRM
                    performHapticFeedback(c)
                    animateScaleTo(1.1f)
                } else if (!activated && wasActivated) {
                    val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE
                            else HapticFeedbackConstants.CLOCK_TICK
                    performHapticFeedback(c)
                    animateScaleTo(1.0f)
                }

                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    if (gestureStarted) {
                        performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                    }
                    activated = false

                    // animate scale back smoothly
                    animateScaleTo(1.0f)

                    // spring return to rest position
                    springVelocity = 0f
                    lastSpringTime = System.nanoTime()
                    isAnimating = true
                    postOnAnimation(springRunnable)
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(springRunnable)
        scaleAnimator?.cancel()
    }
}
