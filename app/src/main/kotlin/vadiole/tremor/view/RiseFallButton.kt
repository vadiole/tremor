package vadiole.tremor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.VibrationEffect
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.TouchEffect
import vadiole.tremor.UiConstants

class RiseFallButton(
    context: Context,
    private val playPrimitive: (primitiveId: Int, scale: Float) -> Unit,
) : View(context), Density {

    private val viewHeight = 72.dp
    private val cornerRadius = UiConstants.CORNER_RADIUS_DP.dp
    private val riseDurationMs = 400L
    private val fallDurationMs = 300L
    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(context, cornerRadius.toInt())
    private val surfaceInset = Floating.surfaceInsetPx(context)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 13f.sp
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val foregroundColor = context.getColor(R.color.foreground)
    private val labelText = context.getString(R.string.example_rise_fall).uppercase()

    private val fillRect = RectF()
    private val clipPath = Path()
    private var fillProgress = 0f
    private var riseAnimator: ValueAnimator? = null
    private var fallAnimator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
        setOnTouchListener(
            TouchEffect(
                pressedScale = 1.02f,
                rubberBandDrag = true,
                maxDragPx = 6f.dp,
                dragDamping = 0.06f,
            ),
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, viewHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fillPaint.shader = LinearGradient(
            0f, h.toFloat(), 0f, 0f,
            (foregroundColor and 0x00FFFFFF) or 0x05000000,
            (foregroundColor and 0x00FFFFFF) or 0x30000000,
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = surfaceInset

        if (fillProgress > 0f) {
            val fillHeight = (height - 2 * halfStroke) * fillProgress
            fillRect.set(halfStroke, height - halfStroke - fillHeight, width - halfStroke, height - halfStroke)

            canvas.save()
            if (surfaceDrawable.copySurfacePath(clipPath)) {
                canvas.clipPath(clipPath)
            }
            canvas.drawRect(fillRect, fillPaint)
            canvas.restore()
        }

        val cx = width / 2f
        val cy = height / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(labelText, cx, cy, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                startRise()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                startFall()
            }
        }
        return true
    }

    private fun startRise() {
        fallAnimator?.cancel()
        riseAnimator?.cancel()

        playPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.8f)

        riseAnimator = ValueAnimator.ofFloat(fillProgress, 1f).apply {
            duration = riseDurationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fillProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startFall() {
        riseAnimator?.cancel()
        fallAnimator?.cancel()

        playPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.8f)

        fallAnimator = ValueAnimator.ofFloat(fillProgress, 0f).apply {
            duration = fallDurationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fillProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        surfaceDrawable.cancelAnimations()
        super.onDetachedFromWindow()
        riseAnimator?.cancel()
        fallAnimator?.cancel()
    }
}
