package vadiole.tremor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import vadiole.tremor.Density
import vadiole.tremor.R

class RiseFallButton(context: Context) : View(context), Density {

    private val viewHeight = 72.dp()
    private val cornerRadius = 6f.dp()
    private val riseDurationMs = 400L
    private val fallDurationMs = 300L

    private val vibrator: Vibrator = run {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 13f.dp()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    private val foregroundColor = context.getColor(R.color.foreground)

    private val rect = RectF()
    private val fillRect = RectF()
    private var fillProgress = 0f
    private var riseAnimator: ValueAnimator? = null
    private var fallAnimator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        if (fillProgress > 0f) {
            val fillHeight = (height - 2 * halfStroke) * fillProgress
            fillRect.set(halfStroke, height - halfStroke - fillHeight, width - halfStroke, height - halfStroke)

            if (fillPaint.shader == null || width > 0) {
                fillPaint.shader = LinearGradient(
                    0f, height.toFloat(), 0f, 0f,
                    (foregroundColor and 0x00FFFFFF) or 0x05000000,
                    (foregroundColor and 0x00FFFFFF) or 0x30000000,
                    Shader.TileMode.CLAMP,
                )
            }

            canvas.save()
            canvas.clipRoundRect(rect, cornerRadius)
            canvas.drawRect(fillRect, fillPaint)
            canvas.restore()
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val label = context.getString(R.string.example_rise_fall)
        val cx = width / 2f
        val cy = height / 2f + textPaint.textSize / 3f
        canvas.drawText(label, cx, cy, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
                startRise()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1f).scaleY(1f).setDuration(150)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2f)).start()
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

    private fun playPrimitive(primitiveId: Int, scale: Float) {
        try {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(primitiveId, scale)
                .compose()
            vibrator.vibrate(effect)
        } catch (_: Exception) {
        }
    }

    private fun Canvas.clipRoundRect(r: RectF, radius: Float) {
        val path = android.graphics.Path()
        path.addRoundRect(r, radius, radius, android.graphics.Path.Direction.CW)
        clipPath(path)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        riseAnimator?.cancel()
        fallAnimator?.cancel()
    }
}
