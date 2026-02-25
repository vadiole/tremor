package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R

class LongPressButton(context: Context) : View(context), Density {

    private val minHeight = 56.dp()
    private val cornerRadius = 6f.dp()
    private val longPressDelay = 500L

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 13f.dp()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    private val pressedColor = context.getColor(R.color.surface_pressed)
    private val normalColor = context.getColor(R.color.surface)

    private val rect = RectF()
    private val progressRect = RectF()
    private var pressStartTime = 0L
    private var isPressed = false
    private var triggered = false
    private val handler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (isPressed && !triggered) {
                invalidate()
                handler.postDelayed(this, 16)
            }
        }
    }

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, minHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        if (isPressed && !triggered) {
            val elapsed = System.currentTimeMillis() - pressStartTime
            val progress = (elapsed.toFloat() / longPressDelay).coerceIn(0f, 1f)
            progressPaint.alpha = (255 * 0.15f).toInt()
            progressRect.set(halfStroke, halfStroke, halfStroke + (width - 2 * halfStroke) * progress, height - halfStroke)
            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint)
            progressPaint.alpha = 255
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val label = context.getString(if (triggered) R.string.example_triggered else R.string.example_hold_me)
        val centerX = width / 2f
        val centerY = height / 2f + textPaint.textSize / 3f
        canvas.drawText(label, centerX, centerY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                triggered = false
                pressStartTime = System.currentTimeMillis()
                bgPaint.color = pressedColor
                animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
                handler.postDelayed(progressRunnable, 16)
                handler.postDelayed({
                    if (isPressed && !triggered) {
                        triggered = true
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        invalidate()
                    }
                }, longPressDelay)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                bgPaint.color = normalColor
                animate().scaleX(1f).scaleY(1f).setDuration(150)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2f)).start()
                handler.removeCallbacksAndMessages(null)
                if (!triggered) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                invalidate()
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }
}
