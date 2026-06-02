package vadiole.tremor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import vadiole.tremor.Density
import vadiole.tremor.R

class HapticToggle(
    context: Context,
    private val onHapticFeedback: (Int) -> Unit,
) : View(context), Density {

    private val trackWidth = 56.dp
    private val trackHeight = 32.dp
    private val thumbRadius = 12f.dp
    private val thumbPadding = 4f.dp
    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(context)
    private val surfaceInset = Floating.surfaceInsetPx(context)

    private var isOn = false
    private var thumbPosition = 0f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        style = Paint.Style.FILL
    }

    private val trackOnColor = context.getColor(R.color.foreground)
    private val thumbOnColor = context.getColor(R.color.background)
    private val thumbOffColor = context.getColor(R.color.foreground)

    private val rect = RectF()
    private var thumbAnimator: ValueAnimator? = null
    private val thumbAnimDurationMs = 200L

    init {
        isClickable = true
        isFocusable = true
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
        setOnClickListener { toggle() }
    }

    fun toggle() {
        isOn = !isOn
        val constant = if (isOn) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
        onHapticFeedback(constant)
        animateThumb()
    }

    private fun animateThumb() {
        thumbAnimator?.cancel()
        val target = if (isOn) 1f else 0f
        thumbAnimator = ValueAnimator.ofFloat(thumbPosition, target).apply {
            duration = thumbAnimDurationMs
            interpolator = thumbInterpolator
            addUpdateListener {
                thumbPosition = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(trackWidth, trackHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val cornerRadius = height / 2f

        trackPaint.color = blendColor(TRANSPARENT, trackOnColor, thumbPosition)
        thumbPaint.color = blendColor(thumbOffColor, thumbOnColor, thumbPosition)

        rect.set(surfaceInset, surfaceInset, width - surfaceInset, height - surfaceInset)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, trackPaint)

        val centerY = height / 2f
        val offX = thumbPadding + thumbRadius + surfaceInset
        val onX = width - thumbPadding - thumbRadius - surfaceInset
        val thumbX = offX + (onX - offX) * thumbPosition
        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbPaint)
    }

    private fun blendColor(from: Int, to: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val fromA = (from shr 24) and 0xFF
        val fromR = (from shr 16) and 0xFF
        val fromG = (from shr 8) and 0xFF
        val fromB = from and 0xFF
        val toA = (to shr 24) and 0xFF
        val toR = (to shr 16) and 0xFF
        val toG = (to shr 8) and 0xFF
        val toB = to and 0xFF
        val a = (fromA + (toA - fromA) * f).toInt()
        val r = (fromR + (toR - fromR) * f).toInt()
        val g = (fromG + (toG - fromG) * f).toInt()
        val b = (fromB + (toB - fromB) * f).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun onDetachedFromWindow() {
        surfaceDrawable.cancelAnimations()
        super.onDetachedFromWindow()
        thumbAnimator?.cancel()
    }

    private companion object {
        const val TRANSPARENT = 0x00000000
        val thumbInterpolator = DecelerateInterpolator(2f)
    }
}
