package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.ViewGroup
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.TouchEffect
import vadiole.tremor.UiConstants

class PrimitiveRow(
    context: Context,
    private val label: String,
    private val constantName: String,
    private val onTrigger: (scale: Float, screenX: Float, screenY: Float) -> Unit,
) : ViewGroup(context), Density {

    private val minRowHeight = 64.dp
    private val cornerRadius = UiConstants.CORNER_RADIUS_DP.dp
    private val padding = 12.dp
    private val textSpacing = 2.dp
    private val drumMarginStart = 8.dp
    private val textEndMargin = 4.dp

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp
    }

    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 13f.sp
        typeface = Typeface.MONOSPACE
        isSubpixelText = true
    }

    private val constantPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = 9f.sp
        typeface = Typeface.MONOSPACE
        isSubpixelText = true
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = 11f.sp
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.RIGHT
        isSubpixelText = true
    }

    private val maxValueWidth = valuePaint.measureText("0.00")

    private val pressedColor = context.getColor(R.color.surface_pressed)
    private val normalColor = context.getColor(R.color.surface)

    private val rect = RectF()
    private val location = IntArray(2)
    private var ellipsizedLabel: CharSequence = label
    private var ellipsizedConstant: CharSequence = constantName

    val drum = DrumRollerView(context).also { addView(it) }

    private var cachedValueText = String.format("%.2f", drum.value)

    init {
        clipChildren = false
        clipToPadding = false
        setWillNotDraw(false)
        setOnTouchListener(
            TouchEffect(
                pressedScale = 1.02f,
                rubberBandDrag = true,
                maxDragPx = 6f.dp,
                dragDamping = 0.06f,
            ),
        )
        drum.onValueChanged = {
            cachedValueText = String.format("%.2f", it)
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val labelHeight = labelPaint.descent() - labelPaint.ascent()
        val constantHeight = constantPaint.descent() - constantPaint.ascent()
        val textBlockHeight = labelHeight + textSpacing + constantHeight
        val rowHeight = maxOf(minRowHeight, (textBlockHeight + 2 * padding).toInt())
        drum.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(rowHeight, MeasureSpec.EXACTLY),
        )
        setMeasuredDimension(width, rowHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val drumW = drum.measuredWidth
        val drumH = drum.measuredHeight
        val drumLeft = (r - l) - drumW
        val drumTop = ((b - t) - drumH) / 2
        drum.layout(drumLeft, drumTop, drumLeft + drumW, drumTop + drumH)
        if (changed) {
            val maxTextWidth = drumLeft - drumMarginStart - maxValueWidth - textEndMargin - padding
            if (maxTextWidth > 0) {
                ellipsizedLabel = TextUtils.ellipsize(label, labelPaint, maxTextWidth, TextUtils.TruncateAt.END)
                ellipsizedConstant = TextUtils.ellipsize(constantName, constantPaint, maxTextWidth, TextUtils.TruncateAt.END)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val labelX = padding.toFloat()
        val labelHeight = labelPaint.descent() - labelPaint.ascent()
        val constantHeight = constantPaint.descent() - constantPaint.ascent()
        val textBlockHeight = labelHeight + textSpacing + constantHeight
        val textBlockTop = (height - textBlockHeight) / 2f
        val labelY = textBlockTop - labelPaint.ascent()
        val constantY = labelY + labelPaint.descent() + textSpacing - constantPaint.ascent()
        canvas.drawText(ellipsizedLabel, 0, ellipsizedLabel.length, labelX, labelY, labelPaint)
        canvas.drawText(ellipsizedConstant, 0, ellipsizedConstant.length, labelX, constantY, constantPaint)

        val valueX = drum.left.toFloat() - drumMarginStart
        val valueY = height / 2f - (valuePaint.ascent() + valuePaint.descent()) / 2f
        canvas.drawText(cachedValueText, valueX, valueY, valuePaint)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isDrumTouch(ev)) return false
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                bgPaint.color = pressedColor
                invalidate()
                getLocationOnScreen(location)
                val screenX = location[0] + event.x
                val screenY = location[1] + event.y
                onTrigger(drum.value, screenX, screenY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                bgPaint.color = normalColor
                invalidate()
            }
        }
        return true
    }

    private fun isDrumTouch(ev: MotionEvent): Boolean {
        val x = ev.x.toInt()
        val y = ev.y.toInt()
        return x >= drum.left && x <= drum.right && y >= drum.top && y <= drum.bottom
    }
}
