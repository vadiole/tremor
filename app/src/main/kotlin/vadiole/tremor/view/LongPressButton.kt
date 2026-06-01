package vadiole.tremor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.TouchEffect
import vadiole.tremor.UiConstants

class LongPressButton(context: Context) : View(context), Density {

    private val minHeight = 56.dp
    private val cornerRadius = UiConstants.CORNER_RADIUS_DP.dp
    private val longPressDelay = 500L

    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(context, cornerRadius.toInt())
    private val surfaceInset = Floating.surfaceInsetPx(context)

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

    private val labelHoldMe = context.getString(R.string.example_hold_me).uppercase()
    private val labelTriggered = context.getString(R.string.example_hold_me_done).uppercase()

    private val progressRect = RectF()
    private val clipPath = Path()
    private var progress = 0f
    private var isHolding = false
    private var triggered = false

    private var progressAnimator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
        setOnTouchListener(
            TouchEffect(
                pressedScale = 1.02f,
                rubberBandDrag = true,
                maxDragPx = 5f.dp,
                dragDamping = 0.06f,
            ),
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, minHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (isHolding && !triggered && progress > 0f) {
            progressPaint.alpha = (255 * 0.15f).toInt()
            progressRect.set(
                surfaceInset,
                surfaceInset,
                surfaceInset + (width - 2 * surfaceInset) * progress,
                height - surfaceInset,
            )
            canvas.save()
            if (surfaceDrawable.copySurfacePath(clipPath)) {
                canvas.clipPath(clipPath)
            }
            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint)
            canvas.restore()
            progressPaint.alpha = 255
        }

        val label = if (triggered) labelTriggered else labelHoldMe
        val centerX = width / 2f
        val centerY = height / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(label, centerX, centerY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isHolding = true
                isPressed = true
                triggered = false
                progress = 0f
                startProgressAnimation()
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isHolding = false
                isPressed = false
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
                if (progress >= 1f && isHolding && !triggered) {
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
        surfaceDrawable.cancelAnimations()
        super.onDetachedFromWindow()
        cancelProgressAnimation()
    }
}
