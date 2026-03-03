package vadiole.tremor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.UiConstants
import vadiole.tremor.ScaleFeedback

class LongPressButton(context: Context) : View(context), Density {

    private val minHeight = 56.dp
    private val cornerRadius = UiConstants.CORNER_RADIUS_DP.dp
    private val longPressDelay = 500L

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 13f.sp
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val pressedColor = context.getColor(R.color.surface_pressed)
    private val normalColor = context.getColor(R.color.surface)

    private val labelHoldMe = context.getString(R.string.example_hold_me)
    private val labelTriggered = context.getString(R.string.example_hold_me_done)

    private val rect = RectF()
    private val progressRect = RectF()
    private var progress = 0f
    private var isPressed = false
    private var triggered = false

    private var progressAnimator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
        setOnTouchListener(ScaleFeedback())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, minHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        if (isPressed && !triggered && progress > 0f) {
            progressPaint.alpha = (255 * 0.15f).toInt()
            progressRect.set(halfStroke, halfStroke, halfStroke + (width - 2 * halfStroke) * progress, height - halfStroke)
            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint)
            progressPaint.alpha = 255
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val label = if (triggered) labelTriggered else labelHoldMe
        val centerX = width / 2f
        val centerY = height / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(label, centerX, centerY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                triggered = false
                progress = 0f
                bgPaint.color = pressedColor
                startProgressAnimation()
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                bgPaint.color = normalColor
                cancelProgressAnimation()
                if (!triggered) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                triggered = false
                progress = 0f
                invalidate()
            }
        }
        return true
    }

    private fun startProgressAnimation() {
        cancelProgressAnimation()
        progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = longPressDelay
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
                if (progress >= 1f && isPressed && !triggered) {
                    triggered = true
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    invalidate()
                }
            }
            start()
        }
    }

    private fun cancelProgressAnimation() {
        progressAnimator?.cancel()
        progressAnimator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelProgressAnimation()
    }
}
