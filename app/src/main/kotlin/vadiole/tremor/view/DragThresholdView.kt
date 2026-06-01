package vadiole.tremor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.UiConstants

class DragThresholdView(context: Context) : View(context), Density {

    private val viewHeight = 72.dp
    private val cornerRadius = UiConstants.CORNER_RADIUS_DP.dp
    private val handleWidth = 36f.dp
    private val handlePadding = 6f.dp
    private val handleCornerRadius = 8f.dp
    private val thresholdFraction = 0.75f
    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(context, cornerRadius.toInt())
    private val surfaceInset = Floating.surfaceInsetPx(context)

    private val thresholdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_muted)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f.dp
        pathEffect = DashPathEffect(floatArrayOf(4f.dp, 4f.dp), 0f)
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface_pressed)
        style = Paint.Style.FILL
    }

    private val handleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp
    }

    private val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_muted)
        strokeWidth = 1.5f.dp
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

    private var thresholdX = 0f
    private var thresholdLineTop = 0f
    private var thresholdLineBottom = 0f

    private var handleScale = 1f
    private var scaleAnimator: ValueAnimator? = null
    private val scaleAnimDurationMs = 250L

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
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, viewHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        handleX = handlePadding

        // a whole number of dashes, vertically centred within the surface inset
        thresholdX = w * thresholdFraction
        val dashLen = 4f.dp
        val gapLen = 4f.dp
        val cycle = dashLen + gapLen
        val lineLength = h - surfaceInset * 2
        val n = ((lineLength + gapLen) / cycle).toInt()
        val patternLen = n * dashLen + (n - 1) * gapLen
        thresholdLineTop = surfaceInset + (lineLength - patternLen) / 2f
        thresholdLineBottom = thresholdLineTop + patternLen
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())

        canvas.drawLine(thresholdX, thresholdLineTop, thresholdX, thresholdLineBottom, thresholdPaint)

        val handleLeft = handleX
        val handleTop = handlePadding
        val handleBottom = height.toFloat() - handlePadding
        handleRect.set(handleLeft, handleTop, handleLeft + handleWidth, handleBottom)

        canvas.save()
        if (surfaceDrawable.copySurfacePath(clipPath)) {
            canvas.clipPath(clipPath)
        }

        if (handleScale != 1f) {
            canvas.scale(handleScale, handleScale, handleRect.centerX(), handleRect.centerY())
        }

        canvas.drawRoundRect(handleRect, handleCornerRadius, handleCornerRadius, handlePaint)
        canvas.drawRoundRect(handleRect, handleCornerRadius, handleCornerRadius, handleBorderPaint)

        // chevron (right-pointing >)
        val chevronCx = handleRect.centerX() + 2f.dp
        val chevronCy = handleRect.centerY()
        val chevronSize = 5f.dp
        canvas.drawLine(chevronCx - chevronSize, chevronCy - chevronSize, chevronCx, chevronCy, chevronPaint)
        canvas.drawLine(chevronCx, chevronCy, chevronCx - chevronSize, chevronCy + chevronSize, chevronPaint)

        canvas.restore()
    }

    private fun animateScaleTo(targetScale: Float) {
        scaleAnimator?.cancel()
        scaleAnimator = ValueAnimator.ofFloat(handleScale, targetScale).apply {
            duration = scaleAnimDurationMs
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
                val touchRight = handleX + handleWidth + 16f.dp
                if (event.x <= touchRight) {
                    isDragging = true
                    isAnimating = false
                    removeCallbacks(springRunnable)
                    dragStartX = event.x
                    handleStartX = handleX
                    gestureStarted = false
                    activated = false
                    isPressed = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) return true

                if (!gestureStarted) {
                    gestureStarted = true
                    performHapticFeedback(HapticFeedbackConstants.DRAG_START)
                }

                val dx = event.x - dragStartX
                handleX = (handleStartX + dx).coerceIn(handlePadding, width - handleWidth - handlePadding)

                val handleRight = handleX + handleWidth
                val wasActivated = activated
                activated = handleRight >= thresholdX

                if (activated && !wasActivated) {
                    performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
                    animateScaleTo(1.1f)
                } else if (!activated && wasActivated) {
                    performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE)
                    animateScaleTo(1.0f)
                }

                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    isPressed = false
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
        surfaceDrawable.cancelAnimations()
        super.onDetachedFromWindow()
        removeCallbacks(springRunnable)
        scaleAnimator?.cancel()
    }
}
