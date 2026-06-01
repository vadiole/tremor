package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.TouchEffect
import vadiole.tremor.UiConstants

class HapticButton(
    context: Context,
    private val label: String,
    private val constantName: String,
    private val isFallback: Boolean = false,
    private val onTrigger: (screenX: Float, screenY: Float) -> Unit,
) : View(context), Density {

    private val minHeight = 56.dp
    private val horizontalPadding = 8f.dp
    private val verticalPadding = 8f.dp

    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(context, UiConstants.CORNER_RADIUS_DP.dp.toInt())

    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 13f.sp
        typeface = Typeface.MONOSPACE
        isSubpixelText = true
    }

    private val constantPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = 8f.sp
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val fallbackPaint = if (isFallback) TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = 7f.sp
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    } else null

    private val constantMetrics = constantPaint.fontMetrics
    private val constantTextHeight = constantMetrics.descent - constantMetrics.ascent
    private val fallbackMetrics = fallbackPaint?.fontMetrics
    private val fallbackTextHeight = fallbackMetrics?.let { it.descent - it.ascent } ?: 0f
    private val gap = 3f.dp
    private val fallbackGap = 1f.dp
    private val fallbackText = if (isFallback) context.getString(R.string.effect_fallback_label) else ""

    private val location = IntArray(2)
    private var labelLayout: StaticLayout? = null
    private var truncatedConstant: String = constantName

    init {
        isClickable = true
        isFocusable = true
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
        setOnTouchListener(
            TouchEffect(
                pressedScale = 1.035f,
                rubberBandDrag = true,
                maxDragPx = 4f.dp,
            ),
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val maxTextWidth = (width - horizontalPadding * 2).toInt().coerceAtLeast(1)

        labelLayout = StaticLayout.Builder.obtain(label, 0, label.length, labelPaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

        truncatedConstant = TextUtils.ellipsize(
            constantName, constantPaint, width - horizontalPadding * 2, TextUtils.TruncateAt.END,
        ).toString()

        val labelHeight = labelLayout!!.height
        val fallbackExtra = if (isFallback) fallbackGap + fallbackTextHeight else 0f
        val totalContentHeight = labelHeight + gap + constantTextHeight + fallbackExtra
        val height = maxOf(minHeight, (totalContentHeight + verticalPadding * 2).toInt())
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val layout = labelLayout ?: return
        val centerX = width / 2f
        val maxTextWidth = (width - horizontalPadding * 2).toInt()
        val fallbackExtra = if (isFallback) fallbackGap + fallbackTextHeight else 0f
        val totalContentHeight = layout.height + gap + constantTextHeight + fallbackExtra

        val labelTop = (height - totalContentHeight) / 2f
        canvas.save()
        canvas.translate(centerX - maxTextWidth / 2f, labelTop)
        layout.draw(canvas)
        canvas.restore()

        val constantY = labelTop + layout.height + gap - constantMetrics.ascent
        canvas.drawText(truncatedConstant, centerX, constantY, constantPaint)

        if (isFallback && fallbackMetrics != null) {
            val fallbackY = constantY + constantMetrics.descent + fallbackGap - fallbackMetrics.ascent
            canvas.drawText(fallbackText, centerX, fallbackY, fallbackPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                getLocationOnScreen(location)
                val screenX = location[0] + event.x
                val screenY = location[1] + event.y
                onTrigger(screenX, screenY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        surfaceDrawable.cancelAnimations()
        super.onDetachedFromWindow()
    }
}
