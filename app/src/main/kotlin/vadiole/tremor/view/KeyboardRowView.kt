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
import android.view.animation.OvershootInterpolator
import vadiole.tremor.Density
import vadiole.tremor.R

class KeyboardRowView(context: Context) : View(context), Density {

    private val keys = charArrayOf('Q', 'W', 'E', 'R', 'T', 'Y')
    private val keyGap = 6f.dp()
    private val keyCorner = 4f.dp()
    private val viewHeight = 56.dp()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 16f.dp()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val normalColor = context.getColor(R.color.surface)
    private val pressedColor = context.getColor(R.color.surface_pressed)

    private val rect = RectF()
    private var pressedIndex = -1

    private val keyScales = FloatArray(keys.size) { 1f }
    private val keyAnimators = arrayOfNulls<ValueAnimator>(keys.size)

    init {
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val totalGaps = (keys.size - 1) * keyGap
        val keyWidth = (width - totalGaps) / keys.size
        val halfStroke = borderPaint.strokeWidth / 2f

        for (i in keys.indices) {
            val left = i * (keyWidth + keyGap)
            val cx = left + keyWidth / 2f
            val cy = height / 2f

            canvas.save()
            canvas.scale(keyScales[i], keyScales[i], cx, cy)

            bgPaint.color = if (i == pressedIndex) pressedColor else normalColor
            rect.set(left + halfStroke, halfStroke, left + keyWidth - halfStroke, height - halfStroke)
            canvas.drawRoundRect(rect, keyCorner, keyCorner, bgPaint)
            canvas.drawRoundRect(rect, keyCorner, keyCorner, borderPaint)

            val textY = cy + textPaint.textSize / 3f
            canvas.drawText(keys[i].toString(), cx, textY, textPaint)

            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressedIndex = keyIndexAt(event.x)
                if (pressedIndex >= 0) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                    animateKeyDown(pressedIndex)
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val newIndex = keyIndexAt(event.x)
                if (newIndex != pressedIndex) {
                    if (pressedIndex >= 0) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
                        animateKeyUp(pressedIndex)
                    }
                    pressedIndex = newIndex
                    if (pressedIndex >= 0) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                        animateKeyDown(pressedIndex)
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (pressedIndex >= 0) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
                    animateKeyUp(pressedIndex)
                }
                pressedIndex = -1
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                if (pressedIndex >= 0) animateKeyUp(pressedIndex)
                pressedIndex = -1
                invalidate()
            }
        }
        return true
    }

    private fun animateKeyDown(index: Int) {
        if (index < 0 || index >= keys.size) return
        keyAnimators[index]?.cancel()
        keyAnimators[index] = ValueAnimator.ofFloat(keyScales[index], 0.85f).apply {
            duration = 80
            addUpdateListener {
                keyScales[index] = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateKeyUp(index: Int) {
        if (index < 0 || index >= keys.size) return
        keyAnimators[index]?.cancel()
        keyAnimators[index] = ValueAnimator.ofFloat(keyScales[index], 1f).apply {
            duration = 150
            interpolator = OvershootInterpolator(2f)
            addUpdateListener {
                keyScales[index] = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun keyIndexAt(x: Float): Int {
        val totalGaps = (keys.size - 1) * keyGap
        val keyWidth = (width - totalGaps) / keys.size
        for (i in keys.indices) {
            val left = i * (keyWidth + keyGap)
            if (x >= left && x <= left + keyWidth) return i
        }
        return -1
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        keyAnimators.forEach { it?.cancel() }
    }
}
