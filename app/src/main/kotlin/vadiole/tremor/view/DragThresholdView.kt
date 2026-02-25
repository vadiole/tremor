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
import android.view.animation.DecelerateInterpolator
import vadiole.tremor.Density
import vadiole.tremor.R

class DragThresholdView(context: Context) : View(context), Density {

    private val viewHeight = 72.dp()
    private val cornerRadius = 6f.dp()
    private val handleWidth = 36f.dp()
    private val handleVerticalPadding = 8f.dp()
    private val handleCornerRadius = 12f.dp()
    private val thresholdFraction = 0.6f
    private val verticalResistance = 0.04f

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
        color = context.getColor(R.color.surface)
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
    private val handlePath = Path()

    private var handleX = 0f
    private var handleY = 0f
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var handleStartX = 0f
    private var activated = false
    private var gestureStarted = false

    // handle pulse animation on threshold crossing
    private var handleScale = 1f
    private var pulseAnimator: ValueAnimator? = null

    // spring animation
    private var springVelocityX = 0f
    private var springVelocityY = 0f
    private var isAnimating = false
    private val springStiffness = 560f
    private val springDamping = 25f

    private val springRunnable = object : Runnable {
        override fun run() {
            if (!isAnimating) return

            val dt = 1f / 60f
            val restX = restHandleX()
            val restY = 0f

            val dxSpring = handleX - restX
            val dySpring = handleY - restY

            val accelX = -springStiffness * dxSpring - springDamping * springVelocityX
            val accelY = -springStiffness * dySpring - springDamping * springVelocityY

            springVelocityX += accelX * dt
            springVelocityY += accelY * dt
            handleX += springVelocityX * dt
            handleY += springVelocityY * dt

            val totalEnergy = dxSpring * dxSpring + dySpring * dySpring +
                springVelocityX * springVelocityX + springVelocityY * springVelocityY

            if (totalEnergy < 0.5f) {
                handleX = restX
                handleY = restY
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

    private fun restHandleX(): Float = borderPaint.strokeWidth / 2f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, viewHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        handleX = restHandleX()
        handleY = 0f
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

        // handle - full height tab with flat left, rounded right
        val handleLeft = handleX
        val handleTop = handleVerticalPadding + handleY
        val handleBottom = height.toFloat() - handleVerticalPadding + handleY
        handleRect.set(handleLeft, handleTop, handleLeft + handleWidth, handleBottom)

        // clip to container bounds
        canvas.save()
        val clipPath = Path()
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(clipPath)

        // apply pulse scale around handle center
        if (handleScale != 1f) {
            canvas.scale(handleScale, handleScale, handleRect.centerX(), handleRect.centerY())
        }

        // flat left corners (0), rounded right corners (12dp)
        handlePath.reset()
        handlePath.addRoundRect(
            handleRect,
            floatArrayOf(0f, 0f, handleCornerRadius, handleCornerRadius, handleCornerRadius, handleCornerRadius, 0f, 0f),
            Path.Direction.CW,
        )
        canvas.drawPath(handlePath, handlePaint)
        canvas.drawPath(handlePath, handleBorderPaint)

        // chevron (right-pointing >)
        val chevronCx = handleRect.centerX() + 2f.dp()
        val chevronCy = handleRect.centerY()
        val chevronSize = 5f.dp()
        canvas.drawLine(chevronCx - chevronSize, chevronCy - chevronSize, chevronCx, chevronCy, chevronPaint)
        canvas.drawLine(chevronCx, chevronCy, chevronCx - chevronSize, chevronCy + chevronSize, chevronPaint)

        canvas.restore()
    }

    private fun pulseHandle() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.08f, 1f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
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
                // touch zone: full height, left side up to handle right edge + padding
                val touchRight = handleX + handleWidth + 16f.dp()
                if (event.x <= touchRight) {
                    isDragging = true
                    isAnimating = false
                    removeCallbacks(springRunnable)
                    dragStartX = event.x
                    dragStartY = event.y
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
                val dy = event.y - dragStartY
                val restX = restHandleX()

                handleX = (handleStartX + dx).coerceIn(restX, width - handleWidth - restX)
                handleY = dy * verticalResistance

                val thresholdX = width * thresholdFraction
                val handleCenter = handleX + handleWidth / 2f
                val wasActivated = activated
                activated = handleCenter >= thresholdX

                if (activated && !wasActivated) {
                    val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
                            else HapticFeedbackConstants.CONFIRM
                    performHapticFeedback(c)
                    pulseHandle()
                } else if (!activated && wasActivated) {
                    val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE
                            else HapticFeedbackConstants.CLOCK_TICK
                    performHapticFeedback(c)
                    pulseHandle()
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
                    springVelocityX = 0f
                    springVelocityY = 0f
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
        pulseAnimator?.cancel()
    }
}
