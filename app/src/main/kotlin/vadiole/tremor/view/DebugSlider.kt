package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.util.Locale
import kotlin.math.abs
import vadiole.tremor.Density
import vadiole.tremor.R

/**
 * A minimal black-and-white scrub slider for the debug tuning panel — a name, a short hint and the
 * live value over a thin track with a draggable thumb. Horizontal drags scrub; vertical drags fall
 * through so the surrounding ScrollView can still scroll; a tap jumps the value to the touch point.
 * The hint is ellipsised so it always keeps at least [valueGap] of clearance before the value.
 * Debug only.
 */
class DebugSlider(
    context: Context,
    private val name: String,
    hint: String,
    private val min: Float,
    private val max: Float,
    initial: Float,
    private val onChange: (Float) -> Unit,
) : View(context), Density {

    private val hintText = if (hint.isEmpty()) "" else "– $hint"
    private val default = initial.coerceIn(min, max)

    private val rowHeight = 42.dp
    private val trackInset = 6f.dp
    private val thumbRadius = 5f.dp
    private val nameHintGap = 8f.dp
    private val valueGap = 36f.dp
    private val baseline = 15f.dp
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var value = default
    private var valueText = format(default)
    private var claimed = false
    private var startX = 0f
    private var startY = 0f

    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = 11f.sp
        typeface = Typeface.MONOSPACE
    }
    private val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_muted)
        textSize = 11f.sp
        typeface = Typeface.MONOSPACE
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 11f.sp
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.RIGHT
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f.dp
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f.dp
        strokeCap = Paint.Cap.ROUND
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        style = Paint.Style.FILL
    }

    init {
        isClickable = true
    }

    fun resetToDefault() {
        value = default
        valueText = format(value)
        onChange(value)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), rowHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val left = trackInset
        val right = width - trackInset
        val trackY = height - 13f.dp

        // value (right-aligned) — formatted only on change, not per frame
        canvas.drawText(valueText, width - trackInset, baseline, valuePaint)

        // name + hint, kept at least valueGap clear of the value
        canvas.drawText(name, left, baseline, namePaint)
        if (hintText.isNotEmpty()) {
            val hintStart = left + namePaint.measureText(name) + nameHintGap
            val valueLeft = (width - trackInset) - valuePaint.measureText(valueText)
            val avail = valueLeft - valueGap - hintStart
            if (avail > 0f) {
                val shown = TextUtils.ellipsize(hintText, hintPaint, avail, TextUtils.TruncateAt.END)
                canvas.drawText(shown, 0, shown.length, hintStart, baseline, hintPaint)
            }
        }

        // track + fill + thumb
        val frac = ((value - min) / (max - min)).coerceIn(0f, 1f)
        val thumbX = left + frac * (right - left)
        canvas.drawLine(left, trackY, right, trackY, trackPaint)
        canvas.drawLine(left, trackY, thumbX, trackY, fillPaint)
        canvas.drawCircle(thumbX, trackY, thumbRadius, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                claimed = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!claimed) {
                    val dx = abs(event.x - startX)
                    val dy = abs(event.y - startY)
                    // claim only a clearly-horizontal drag; leave vertical drags to the ScrollView
                    if (dx > touchSlop && dx >= dy) {
                        claimed = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
                if (claimed) setFromX(event.x)
            }
            MotionEvent.ACTION_UP -> {
                if (!claimed && abs(event.x - startX) < touchSlop && abs(event.y - startY) < touchSlop) {
                    setFromX(event.x) // tap to set
                }
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    private fun setFromX(x: Float) {
        val left = trackInset
        val right = width - trackInset
        val frac = ((x - left) / (right - left)).coerceIn(0f, 1f)
        value = min + frac * (max - min)
        valueText = format(value)
        onChange(value)
        invalidate()
    }

    private fun format(v: Float): String = when {
        max <= 1.5f -> String.format(Locale.US, "%.2f", v)
        max <= 20f -> String.format(Locale.US, "%.1f", v)
        else -> String.format(Locale.US, "%.0f", v)
    }
}
