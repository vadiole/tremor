package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R

class DragThresholdView(context: Context) : View(context), Density {

    private val viewHeight = 72.dp()
    private val cornerRadius = 6f.dp()
    private val handleWidth = 48f.dp()
    private val handleHeight = 40f.dp()
    private val handleCorner = 4f.dp()
    private val thresholdFraction = 0.6f
    private val handlePadding = 8f.dp()
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
        color = context.getColor(R.color.foreground)
        style = Paint.Style.FILL
    }

    private val handleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val gripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.background)
        strokeWidth = 1.5f.dp()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val activatedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private val handleRect = RectF()

    private var handleX = 0f
    private var handleY = 0f
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var handleStartX = 0f
    private var activated = false
    private var gestureStarted = false

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
            val restX = handlePadding
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, viewHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        handleX = handlePadding
        handleY = 0f
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // activated fill
        if (activated) {
            val fillWidth = handleX + handleWidth / 2f
            activatedPaint.alpha = 20
            canvas.save()
            val clipPath = android.graphics.Path()
            clipPath.addRoundRect(rect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
            canvas.clipPath(clipPath)
            canvas.drawRect(halfStroke, halfStroke, fillWidth, height - halfStroke, activatedPaint)
            canvas.restore()
            activatedPaint.alpha = 255
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // threshold line
        val thresholdX = width * thresholdFraction
        val lineTop = 12f.dp()
        val lineBottom = height - 12f.dp()
        canvas.drawLine(thresholdX, lineTop, thresholdX, lineBottom, thresholdPaint)

        // handle
        val centerY = height / 2f + handleY
        val handleLeft = handleX
        val handleTop = centerY - handleHeight / 2f
        handleRect.set(handleLeft, handleTop, handleLeft + handleWidth, handleTop + handleHeight)
        canvas.drawRoundRect(handleRect, handleCorner, handleCorner, handlePaint)
        canvas.drawRoundRect(handleRect, handleCorner, handleCorner, handleBorderPaint)

        // grip lines on handle
        val gripCx = handleLeft + handleWidth / 2f
        val gripCy = centerY
        val gripSpacing = 4f.dp()
        for (i in -1..1) {
            val gx = gripCx + i * gripSpacing
            canvas.drawLine(gx, gripCy - 6f.dp(), gx, gripCy + 6f.dp(), gripPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val centerY = height / 2f + handleY
                val touchLeft = handleX - 16f.dp()
                val touchRight = handleX + handleWidth + 16f.dp()
                val touchTop = centerY - handleHeight / 2f - 16f.dp()
                val touchBottom = centerY + handleHeight / 2f + 16f.dp()

                if (event.x in touchLeft..touchRight && event.y in touchTop..touchBottom) {
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
                    performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
                }

                val dx = event.x - dragStartX
                val dy = event.y - dragStartY

                handleX = (handleStartX + dx).coerceIn(handlePadding, width - handleWidth - handlePadding)
                handleY = dy * verticalResistance

                val thresholdX = width * thresholdFraction
                val handleCenter = handleX + handleWidth / 2f
                val wasActivated = activated
                activated = handleCenter >= thresholdX

                if (activated && !wasActivated) {
                    performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else if (!activated && wasActivated) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
    }
}
